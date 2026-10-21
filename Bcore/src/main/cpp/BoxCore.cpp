//
// Created by Milk on 4/9/21.
//

#include "BoxCore.h"
#include "Log.h"
#include "IO.h"
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/UnixFileSystemHook.h>
#include <Hook/BinderHook.h>
#include <Hook/RuntimeHook.h>
#include <Hook/AntiDetectFix.h>
#include <Hook/FileSystemHook.h>
#include "Utils/HexDump.h"
#include <map>
#include <mutex>
#include <unistd.h>

namespace {
    std::mutex sPidMapLock;
    std::map<pid_t, int> sGuestPidMap;
    int sSelfGuestUid = -1;
}

struct {
    JavaVM *vm;
    jclass NativeCoreClass;
    jmethodID getCallingUidId;
    jmethodID redirectPathString;
    jmethodID redirectPathFile;
    jmethodID loadEmptyDex;
    jmethodID loadEmptyDexL;
    jclass BinderClass;
    jmethodID getCallingPidId;
    int api_level;
} VMEnv;


JNIEnv *getEnv() {
    JNIEnv *env;
    VMEnv.vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *ensureEnvCreated() {
    JNIEnv *env = getEnv();
    if (env == NULL) {
        VMEnv.vm->AttachCurrentThread(&env, NULL);
    }
    return env;
}

int BoxCore::getCallingUid(JNIEnv *env, int orig) {
    env = ensureEnvCreated();
    // resolve the binder caller; outside a transaction this is our own pid,
    // which makes the guest self-identity lookup below the common case
    if (VMEnv.BinderClass != NULL) {
        jint callingPid = env->CallStaticIntMethod(VMEnv.BinderClass, VMEnv.getCallingPidId);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            callingPid = -1;
        }
        if (callingPid > 0) {
            int guestUid = getGuestUidByPid(callingPid);
            if (guestUid > 0) {
                return guestUid;
            }
            int selfUid = getSelfGuestUid();
            if (selfUid > 0 && callingPid == getpid()) {
                return selfUid;
            }
        }
    }
    return env->CallStaticIntMethod(VMEnv.NativeCoreClass, VMEnv.getCallingUidId, orig);
}

void BoxCore::putGuestPid(int pid, int uid) {
    std::lock_guard<std::mutex> guard(sPidMapLock);
    sGuestPidMap[pid] = uid;
}

void BoxCore::removeGuestPid(int pid) {
    std::lock_guard<std::mutex> guard(sPidMapLock);
    sGuestPidMap.erase(pid);
}

void BoxCore::setSelfUid(int uid) {
    sSelfGuestUid = uid;
}

int BoxCore::getGuestUidByPid(int pid) {
    std::lock_guard<std::mutex> guard(sPidMapLock);
    std::map<pid_t, int>::iterator it = sGuestPidMap.find(pid);
    return it == sGuestPidMap.end() ? -1 : it->second;
}

int BoxCore::getSelfGuestUid() {
    return sSelfGuestUid;
}

jstring BoxCore::redirectPathString(JNIEnv *env, jstring path) {
    env = ensureEnvCreated();
    return (jstring) env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathString, path);
}

jobject BoxCore::redirectPathFile(JNIEnv *env, jobject path) {
    env = ensureEnvCreated();
    return env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathFile, path);
}

jlongArray BoxCore::loadEmptyDex(JNIEnv *env) {
    env = ensureEnvCreated();
    return (jlongArray) env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.loadEmptyDex);
}

int BoxCore::getApiLevel() {
    return VMEnv.api_level;
}

JavaVM *BoxCore::getJavaVM() {
    return VMEnv.vm;
}

void nativeHook(JNIEnv *env) {
    BaseHook::init(env);
    UnixFileSystemHook::init(env);
    VMClassLoaderHook::init(env);
    RuntimeHook::init(env);
    BinderHook::init(env);
    FileSystemHook::install(env);
    FileSystemHook::installDobbyHooks();
    FileSystemHook::installThreadFreeze();
}

void hideXposed(JNIEnv *env, jclass clazz) {
    ALOGD("set hideXposed");
    VMClassLoaderHook::hideXposed();
}

void init(JNIEnv *env, jobject clazz, jint api_level) {
    ALOGD("NativeCore init.");
    VMEnv.api_level = api_level;
    VMEnv.NativeCoreClass = (jclass) env->NewGlobalRef(env->FindClass(VMCORE_CLASS));
    VMEnv.getCallingUidId = env->GetStaticMethodID(VMEnv.NativeCoreClass, "getCallingUid", "(I)I");
    VMEnv.BinderClass = (jclass) env->NewGlobalRef(env->FindClass("android/os/Binder"));
    VMEnv.getCallingPidId = env->GetStaticMethodID(VMEnv.BinderClass, "getCallingPid", "()I");
    VMEnv.redirectPathString = env->GetStaticMethodID(VMEnv.NativeCoreClass, "redirectPath",
                                                      "(Ljava/lang/String;)Ljava/lang/String;");
    VMEnv.redirectPathFile = env->GetStaticMethodID(VMEnv.NativeCoreClass, "redirectPath",
                                                    "(Ljava/io/File;)Ljava/io/File;");
    VMEnv.loadEmptyDex = env->GetStaticMethodID(VMEnv.NativeCoreClass, "loadEmptyDex",
                                                "()[J");

    JniHook::InitJniHook(env, api_level);
}

void addIORule(JNIEnv *env, jclass clazz, jstring target_path,
               jstring relocate_path) {
    IO::addRule(env->GetStringUTFChars(target_path, JNI_FALSE),
                env->GetStringUTFChars(relocate_path, JNI_FALSE));
}

void enableIO(JNIEnv *env, jclass clazz) {
    IO::init(env);
    nativeHook(env);
}

void patchLoaderLib(JNIEnv *env, jclass clazz, jstring path) {
    const char *pathC = env->GetStringUTFChars(path, JNI_FALSE);
    AntiDetectFix::patchAll(pathC);
    env->ReleaseStringUTFChars(path, pathC);
}

// path is kept for parity with the ZOLV native interface; the map is in-memory
void nativePidMapPut(JNIEnv *env, jclass clazz, jstring path, jint pid, jint uid) {
    BoxCore::putGuestPid(pid, uid);
}

void nativePidMapRemove(JNIEnv *env, jclass clazz, jstring path, jint pid) {
    BoxCore::removeGuestPid(pid);
}

void nativeSetSelfUid(JNIEnv *env, jclass clazz, jint uid) {
    BoxCore::setSelfUid(uid);
}

static JNINativeMethod gMethods[] = {
        {"hideXposed", "()V",                                     (void *) hideXposed},
        {"addIORule",  "(Ljava/lang/String;Ljava/lang/String;)V", (void *) addIORule},
        {"enableIO",   "()V",                                     (void *) enableIO},
        {"init",       "(I)V",                                    (void *) init},
        {"patchLoaderLib", "(Ljava/lang/String;)V",               (void *) patchLoaderLib},
        {"pidMapPut",    "(Ljava/lang/String;II)V",               (void *) nativePidMapPut},
        {"pidMapRemove", "(Ljava/lang/String;I)V",                (void *) nativePidMapRemove},
        {"setSelfUid",   "(I)V",                                  (void *) nativeSetSelfUid},
};

int registerNativeMethods(JNIEnv *env, const char *className,
                          JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, VMCORE_CLASS, gMethods,
                               sizeof(gMethods) / sizeof(gMethods[0])))
        return JNI_FALSE;
    return JNI_TRUE;
}

void registerMethod(JNIEnv *jenv) {
    registerNatives(jenv);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    VMEnv.vm = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_EVERSION;
    }
    registerMethod(env);
    return JNI_VERSION_1_6;
}