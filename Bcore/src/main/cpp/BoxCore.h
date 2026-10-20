//
// Created by Milk on 4/9/21.
//

#ifndef VIRTUALM_VMCORE_H
#define VIRTUALM_VMCORE_H

#include <jni.h>

#define VMCORE_CLASS "top/niunaijun/blackbox/core/NativeCore"

class BoxCore {
public:
    static JavaVM *getJavaVM();
    static int getApiLevel();
    static int getCallingUid(JNIEnv *env, int orig);
    static jstring redirectPathString(JNIEnv *env, jstring path);
    static jobject redirectPathFile(JNIEnv *env, jobject path);
    static jlongArray loadEmptyDex(JNIEnv *env);

    // guest identity: pid -> virtual uid map plus this process's own guest uid
    static void putGuestPid(int pid, int uid);
    static void removeGuestPid(int pid);
    static void setSelfUid(int uid);
    static int getGuestUidByPid(int pid);
    static int getSelfGuestUid();
};


#endif //VIRTUALM_VMCORE_H
