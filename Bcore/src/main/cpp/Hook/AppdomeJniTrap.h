#ifndef BLACKBOX_APPDOME_JNI_TRAP_H
#define BLACKBOX_APPDOME_JNI_TRAP_H

#include <jni.h>

// Surgical prevention of the guest protection's environment probes: instead
// of surviving Appdome's kill paths (seccomp/signal hacks — removed), we
// intercept the JNI calls the loader uses to inspect the environment and
// answer them with clean-environment values. Port of ZOLVS AppdomeJniTrap.
class AppdomeJniTrap {
public:
    // Dobby-hook the JNIEnv function-table entries the protection uses.
    // Idempotent. Safe to call on every libloader load.
    static void install(JNIEnv *env);
};

#endif //BLACKBOX_APPDOME_JNI_TRAP_H
