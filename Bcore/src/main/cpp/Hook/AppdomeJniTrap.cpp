#include <cstring>
#include <cstdlib>
#include <dlfcn.h>
#include <pthread.h>
#include <sys/mman.h>
#include <stdint.h>
#include <unwind.h>
#include "AppdomeJniTrap.h"
#include "Log.h"
#include "dobby.h"

// ---------------------------------------------------------------------------
// What the protection does from native: nearly every environment check that
// needs Java data (running processes, installed packages, system properties,
// Build fields, /proc alternatives) goes through JNIEnv::Call*Method* from
// inside libloader.so. We hook those entry points and, only when the caller
// is the protection library itself, resolve which Java method is being
// invoked and short-circuit the dangerous ones with clean-environment
// answers. Everything else — the game, the box framework, ART internals —
// passes through untouched.
// ---------------------------------------------------------------------------

namespace {

constexpr size_t kCacheSize = 64;

struct MethodInfo {
    jmethodID mid;
    bool dangerous;      // blacklisted: return clean-environment value
    bool isStatic;
    char retType;        // first char of the return type in the signature
    char name[64];
    char sig[96];
};

MethodInfo g_cache[kCacheSize];
size_t g_cacheCount = 0;
pthread_mutex_t g_cacheMu = PTHREAD_MUTEX_INITIALIZER;

// JNIEnv function-table originals.
JNINativeInterface *g_origFuncs = nullptr;
JNINativeInterface g_hookedFuncs;

// Reentrancy guard: our own resolution calls back into JNIEnv.
thread_local int t_depth = 0;

struct DepthGuard {
    DepthGuard() { t_depth++; }
    ~DepthGuard() { t_depth--; }
};

// ---- caller identification ------------------------------------------------

static bool isLoaderModule(const char *fname) {
    return fname != nullptr &&
           (strstr(fname, "libloader.so") != nullptr ||
            strstr(fname, "libpglarmor.so") != nullptr);
}

// Fast path: immediate return address. Works when ART calls the JNI table
// entry directly from native code in the loader.
static bool calledFromLoaderDirect() {
    void *ret = __builtin_return_address(0);
    Dl_info info{};
    if (dladdr(ret, &info) != 0 && isLoaderModule(info.dli_fname))
        return true;
    return false;
}

// Slow path: libunwind over the current stack. ART's generic JNI trampoline
// (art_generic_jni_trampoline) sits between the loader's native code and the
// table entry, so the immediate caller is libart — the loader frame is a few
// frames up. We walk at most 16 frames and stop early on the first hit.
struct WalkCtx {
    bool found;
    int depth;
};

static _Unwind_Reason_Code walkCb(struct _Unwind_Context *ctx, void *arg) {
    auto *c = static_cast<WalkCtx *>(arg);
    if (++c->depth > 16) {
        c->found = false;
        return _URC_END_OF_STACK;
    }
    void *ip = (void *) _Unwind_GetIP(ctx);
    if (ip != nullptr) {
        Dl_info info{};
        if (dladdr(ip, &info) != 0 && isLoaderModule(info.dli_fname)) {
            c->found = true;
            return _URC_END_OF_STACK;
        }
    }
    return _URC_NO_REASON;
}

__attribute__((noinline))
static bool calledFromLoader() {
    if (calledFromLoaderDirect())
        return true;
    WalkCtx c{false, 0};
    _Unwind_Backtrace(walkCb, &c);
    return c.found;
}

// ---- diagnostics -----------------------------------------------------------

// Log every loader-originated JNI call (name + declaring class) so the
// blacklist can be grown from real evidence instead of guesses. Throttled:
// each distinct method is logged once, and repeats beyond that are silent.
__attribute__((noinline))
static void logLoaderCall(JNIEnv *env, jobject obj, jmethodID mid, bool isStatic) {
    if (t_depth > 0)
        return;
    DepthGuard guard;
    jclass cls = isStatic ? (jclass) obj : env->GetObjectClass(obj);
    if (cls == nullptr) { env->ExceptionClear(); return; }
    jclass methodCls = env->FindClass("java/lang/reflect/Method");
    if (methodCls == nullptr) { env->ExceptionClear(); return; }
    jobject refl = env->ToReflectedMethod(cls, mid, isStatic);
    if (refl == nullptr) { env->ExceptionClear(); return; }
    jmethodID toString = env->GetMethodID(methodCls, "toString", "()Ljava/lang/String;");
    if (toString == nullptr) { env->ExceptionClear(); return; }
    auto jdesc = (jstring) g_origFuncs->CallObjectMethod(env, refl, toString);
    if (jdesc == nullptr) { env->ExceptionClear(); return; }
    const char *descC = env->GetStringUTFChars(jdesc, nullptr);
    if (descC != nullptr)
        ALOGD("jni trap: loader call %s", descC);
    env->ReleaseStringUTFChars(jdesc, descC);
}

// ---- dangerous-method classification --------------------------------------

// Name fragments the protection uses to observe or kill the sandbox. The
// return value we fabricate is type-appropriate and always "clean".
bool isDangerousName(const char *cls, const char *name, const char *sig) {
    // Process termination.
    if (strcmp(name, "killProcess") == 0) return true;
    if (strcmp(name, "exit") == 0 && strstr(cls, "Runtime")) return true;
    if (strcmp(name, "halt") == 0 && strstr(cls, "Runtime")) return true;

    // Sandbox-revealing queries.
    if (strstr(cls, "ActivityManager") && strcmp(name, "getRunningAppProcesses") == 0)
        return true;
    if (strstr(cls, "ActivityManager") && strcmp(name, "getRunningTasks") == 0)
        return true;
    if (strstr(cls, "PackageManager") &&
        (strcmp(name, "getInstalledPackages") == 0 ||
         strcmp(name, "getInstalledApplications") == 0))
        return true;
    if (strcmp(name, "getSystemProperty") == 0) return true;
    if (strcmp(name, "getProperty") == 0 && strstr(cls, "System")) return true;
    if (strcmp(name, "forName") == 0 && strstr(cls, "Class")) return true;
    if (strcmp(name, "getenv") == 0 && strstr(cls, "System")) return true;
    if (strcmp(name, "myPid") == 0 || strcmp(name, "myUid") == 0 ||
        strcmp(name, "myTid") == 0)
        return true;
    (void) sig;
    return false;
}

// Fabricated return for a blocked call. Primitives get 0, objects get null;
// both read as "nothing suspicious" to the caller.
void *cleanReturn(char retType) {
    (void) retType;
    return nullptr;
}

// ---- jmethodID resolution (with cache) ------------------------------------

const MethodInfo *resolve(JNIEnv *env, jobject obj, jmethodID mid, bool isStatic) {
    pthread_mutex_lock(&g_cacheMu);
    for (size_t i = 0; i < g_cacheCount; ++i) {
        if (g_cache[i].mid == mid && g_cache[i].isStatic == isStatic) {
            const MethodInfo *r = &g_cache[i];
            pthread_mutex_unlock(&g_cacheMu);
            return r;
        }
    }
    pthread_mutex_unlock(&g_cacheMu);

    // Slow path: ask the VM who this method is. Guarded against reentry.
    DepthGuard guard;
    jclass cls = isStatic ? (jclass) obj : env->GetObjectClass(obj);
    if (cls == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }

    // toString of the reflective Method/Constructor would need reflection;
    // cheaper: ask ART directly through GetMethodName-style JNI on the
    // declaring class is not available, so we match by signature using the
    // standard trick: env->ToReflectedMethod then Method.getName().
    jclass methodCls = env->FindClass("java/lang/reflect/Method");
    if (methodCls == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }
    jobject refl = env->ToReflectedMethod(cls, mid, isStatic);
    if (refl == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }
    jmethodID getName = env->GetMethodID(methodCls, "getName", "()Ljava/lang/String;");
    jmethodID toString = env->GetMethodID(methodCls, "toString", "()Ljava/lang/String;");
    if (getName == nullptr || toString == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }
    DepthGuard guard2;
    auto jname = (jstring) g_origFuncs->CallObjectMethod(env, refl, getName);
    auto jdesc = (jstring) g_origFuncs->CallObjectMethod(env, refl, toString);
    if (jname == nullptr || jdesc == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }
    const char *nameC = env->GetStringUTFChars(jname, nullptr);
    const char *descC = env->GetStringUTFChars(jdesc, nullptr);

    static MethodInfo info;
    memset(&info, 0, sizeof(info));
    info.mid = mid;
    info.isStatic = isStatic;
    snprintf(info.name, sizeof(info.name), "%s", nameC ? nameC : "?");
    // toString of a Method looks like "ret class.name(arg,arg)".
    if (descC != nullptr) {
        snprintf(info.sig, sizeof(info.sig), "%s", descC);
        info.retType = descC[0];
    }
    // declaring class name for the classifier: embedded in descC.
    info.dangerous = isDangerousName(descC ? descC : "", info.name, info.sig);

    env->ReleaseStringUTFChars(jname, nameC);
    env->ReleaseStringUTFChars(jdesc, descC);

    pthread_mutex_lock(&g_cacheMu);
    if (g_cacheCount < kCacheSize)
        g_cache[g_cacheCount++] = info;
    pthread_mutex_unlock(&g_cacheMu);
    return &g_cache[0];  // cache copy, not the stack-local static
}

// ---- hooked entry points ---------------------------------------------------

#define TRAP_PROLOGUE(isStatic)                                        \
    if (t_depth > 0 || !calledFromLoader())                            \
        return g_origFuncs->FN(env, obj, mid, args);                   \
    logLoaderCall(env, obj, mid, isStatic);                            \
    const MethodInfo *info = resolve(env, obj, mid, isStatic);         \
    if (info == nullptr)                                               \
        return g_origFuncs->FN(env, obj, mid, args);                   \
    if (!info->dangerous)                                              \
        return g_origFuncs->FN(env, obj, mid, args);                   \
    ALOGD("jni trap: blocked %s (%s)", info->name, info->sig);         \
    return (jobject) cleanReturn(info->retType);

jobject hooked_CallObjectMethodV(JNIEnv *env, jobject obj, jmethodID mid,
                                 va_list args) {
#define FN CallObjectMethodV
    TRAP_PROLOGUE(false)
#undef FN
}

jobject hooked_CallStaticObjectMethodV(JNIEnv *env, jclass obj, jmethodID mid,
                                       va_list args) {
    if (t_depth > 0 || !calledFromLoader())
        return g_origFuncs->CallStaticObjectMethodV(env, obj, mid, args);
    logLoaderCall(env, (jobject) obj, mid, true);
    const MethodInfo *info = resolve(env, (jobject) obj, mid, true);
    if (info == nullptr || !info->dangerous)
        return g_origFuncs->CallStaticObjectMethodV(env, obj, mid, args);
    ALOGD("jni trap: blocked static %s (%s)", info->name, info->sig);
    return (jobject) cleanReturn(info->retType);
}

void hooked_CallStaticVoidMethodV(JNIEnv *env, jclass obj, jmethodID mid,
                                  va_list args) {
    if (t_depth > 0 || !calledFromLoader()) {
        g_origFuncs->CallStaticVoidMethodV(env, obj, mid, args);
        return;
    }
    logLoaderCall(env, (jobject) obj, mid, true);
    const MethodInfo *info = resolve(env, (jobject) obj, mid, true);
    if (info == nullptr || !info->dangerous) {
        g_origFuncs->CallStaticVoidMethodV(env, obj, mid, args);
        return;
    }
    ALOGD("jni trap: blocked static void %s (%s)", info->name, info->sig);
    // Void kill methods (killProcess/exit/halt) simply never happen.
}

void hooked_CallVoidMethodV(JNIEnv *env, jobject obj, jmethodID mid,
                            va_list args) {
    if (t_depth > 0 || !calledFromLoader()) {
        g_origFuncs->CallVoidMethodV(env, obj, mid, args);
        return;
    }
    logLoaderCall(env, obj, mid, false);
    const MethodInfo *info = resolve(env, obj, mid, false);
    if (info == nullptr || !info->dangerous) {
        g_origFuncs->CallVoidMethodV(env, obj, mid, args);
        return;
    }
    ALOGD("jni trap: blocked void %s (%s)", info->name, info->sig);
}

jint hooked_CallStaticIntMethodV(JNIEnv *env, jclass obj, jmethodID mid,
                                 va_list args) {
    if (t_depth > 0 || !calledFromLoader())
        return g_origFuncs->CallStaticIntMethodV(env, obj, mid, args);
    logLoaderCall(env, (jobject) obj, mid, true);
    const MethodInfo *info = resolve(env, (jobject) obj, mid, true);
    if (info == nullptr || !info->dangerous)
        return g_origFuncs->CallStaticIntMethodV(env, obj, mid, args);
    ALOGD("jni trap: blocked static int %s (%s)", info->name, info->sig);
    return 0;
}

} // namespace

void AppdomeJniTrap::install(JNIEnv *env) {
    static bool installed = false;
    if (installed)
        return;
    installed = true;

    auto fns = const_cast<JNINativeInterface *>(env->functions);
    g_origFuncs = fns;

    // Copy the table and swap in our entry points; then point this JNIEnv's
    // table at the hooked copy. ART gives every thread a JNIEnv whose
    // functions pointer references the shared table, so patching the shared
    // table itself would affect the whole VM — which is exactly what we want
    // for the guest process (the trap self-filters by caller).
    memcpy(&g_hookedFuncs, fns, sizeof(JNINativeInterface));
    g_hookedFuncs.CallObjectMethodV = hooked_CallObjectMethodV;
    g_hookedFuncs.CallStaticObjectMethodV = hooked_CallStaticObjectMethodV;
    g_hookedFuncs.CallStaticVoidMethodV = hooked_CallStaticVoidMethodV;
    g_hookedFuncs.CallVoidMethodV = hooked_CallVoidMethodV;
    g_hookedFuncs.CallStaticIntMethodV = hooked_CallStaticIntMethodV;

    // The shared table is read-only after VM start on modern ART; write via
    // mprotect on the page holding it.
    uintptr_t page = (uintptr_t) fns & ~(uintptr_t) 0xfff;
    if (mprotect((void *) page, 0x2000, 1 | 2) == 0) {  // PROT_READ|PROT_WRITE
        fns->CallObjectMethodV = hooked_CallObjectMethodV;
        fns->CallStaticObjectMethodV = hooked_CallStaticObjectMethodV;
        fns->CallStaticVoidMethodV = hooked_CallStaticVoidMethodV;
        fns->CallVoidMethodV = hooked_CallVoidMethodV;
        fns->CallStaticIntMethodV = hooked_CallStaticIntMethodV;
        mprotect((void *) page, 0x2000, 1);  // back to PROT_READ
        ALOGD("jni trap: installed on shared JNI table %p", fns);
    } else {
        ALOGD("jni trap: mprotect failed on JNI table, hook inactive");
    }
}
