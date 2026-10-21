//
// Created by Milk on 5/5/21.
//

#include "RuntimeHook.h"
#import "JniHook/JniHook.h"
#include "BoxCore.h"
#include "AntiDetectFix.h"
#include <Hook/FileSystemHook.h>
#include <cstring>

HOOK_JNI(jstring, nativeLoad, JNIEnv *env, jobject obj, jstring name, jobject class_loader) {
    const char *nameC = env->GetStringUTFChars(name, JNI_FALSE);
    ALOGD("nativeLoad: %s", nameC);
    if (strstr(nameC, "libloader")) {
        // Everything goes in BEFORE dlopen: the protection kills the process
        // within ~20 ms of loading, long before any post-load hook would run.
        AntiDetectFix::installSyscallGuard();
        AntiDetectFix::installCrashShield();
        AntiDetectFix::installStopShield();
        AntiDetectFix::installSignalGuard();
        AntiDetectFix::installGlobalFaultSwallow();
        AntiDetectFix::installJavaKillShield(env);
        // exit/_exit freezing is safe pre-load (only used on termination);
        // anon-exec freezing is NOT (it wedges libart's interpreter), so it
        // runs post-load inside installInstructionTrap's scoped filter.
        AntiDetectFix::installInstructionTrap();
    }
    AntiDetectFix::patchFileOnDisk(nameC);
    AntiDetectFix::patchSyscallsOnDisk(nameC);
    AntiDetectFix::patchSvcStubsOnDisk(nameC);
    jstring result = orig_nativeLoad(env, obj, name, class_loader);
    if (result == nullptr) {
        AntiDetectFix::onLibraryLoaded(nameC);
        FileSystemHook::patchAllModules();
    }
    env->ReleaseStringUTFChars(name, nameC);
    return result;
}

HOOK_JNI(jstring, nativeLoad2, JNIEnv *env, jobject obj, jstring name, jobject class_loader,
         jobject caller) {
    const char *nameC = env->GetStringUTFChars(name, JNI_FALSE);
    ALOGD("nativeLoad: %s", nameC);
    if (strstr(nameC, "libloader")) {
        // Everything goes in BEFORE dlopen: the protection kills the process
        // within ~20 ms of loading, long before any post-load hook would run.
        AntiDetectFix::installSyscallGuard();
        AntiDetectFix::installCrashShield();
        AntiDetectFix::installStopShield();
        AntiDetectFix::installSignalGuard();
        AntiDetectFix::installGlobalFaultSwallow();
        AntiDetectFix::installJavaKillShield(env);
        // exit/_exit freezing is safe pre-load (only used on termination);
        // anon-exec freezing is NOT (it wedges libart's interpreter), so it
        // runs post-load inside installInstructionTrap's scoped filter.
        AntiDetectFix::installInstructionTrap();
    }
    AntiDetectFix::patchFileOnDisk(nameC);
    AntiDetectFix::patchSyscallsOnDisk(nameC);
    AntiDetectFix::patchSvcStubsOnDisk(nameC);
    jstring result = orig_nativeLoad2(env, obj, name, class_loader, caller);
    if (result == nullptr) {
        AntiDetectFix::onLibraryLoaded(nameC);
        FileSystemHook::patchAllModules();
    }
    env->ReleaseStringUTFChars(name, nameC);
    return result;
}

void RuntimeHook::init(JNIEnv *env) {
    const char *className = "java/lang/Runtime";
    if (BoxCore::getApiLevel() >= __ANDROID_API_Q__) {
        JniHook::HookJniFun(env, className, "nativeLoad",
                            "(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/Class;)Ljava/lang/String;",
                            (void *) new_nativeLoad2,
                            (void **) (&orig_nativeLoad2), true);
    } else {
        JniHook::HookJniFun(env, className, "nativeLoad",
                            "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/String;",
                            (void *) new_nativeLoad,
                            (void **) (&orig_nativeLoad), true);
    }
}
