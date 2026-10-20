#include <sys/types.h>
#include <pthread.h>
#include <jni.h>
#include <string>
#include <jni.h>
#include <unistd.h>
#include <sys/system_properties.h>
#include <dlfcn.h>
#include "backend/Login.h"

/* LOGIN HERE */
int Register1(JNIEnv *env) {
    JNINativeMethod methods[] = {{"Check", "(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;", (void *) native_Check}};
    jclass clazz = env->FindClass("com/dms/container/LoginActivity");
    if (!clazz)
        return -1;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0)
        return -1;
    return 0;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (Register1(env) != 0)
        return -1;
    return JNI_VERSION_1_6;
}


