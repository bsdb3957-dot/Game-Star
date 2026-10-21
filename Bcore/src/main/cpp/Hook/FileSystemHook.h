//
// Native GOT/PLT hook layer hiding the host box from guest protection code.
//

#ifndef VIRTUALM_FILESYSTEMHOOK_H
#define VIRTUALM_FILESYSTEMHOOK_H

#include <jni.h>

class FileSystemHook {
public:
    // Patch libc GOT entries of every module loaded so far; call once
    // after the native hooks are in place.
    static void install(JNIEnv *env);

    // Re-patch all currently loaded modules; call after each guest
    // nativeLoad so freshly dlopened libraries are covered too.
    static void patchAllModules();

    // Dobby inline hooks for paths GOT patching cannot reach: raw read()
    // of /proc/self/cmdline/maps and protection-internal direct calls.
    static void installDobbyHooks();

    // Park watchdog threads spawned from the protection library.
    static void installThreadFreeze();
};

#endif //VIRTUALM_FILESYSTEMHOOK_H
