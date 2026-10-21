#ifndef BLACKBOX_ANTIDETECTFIX_H
#define BLACKBOX_ANTIDETECTFIX_H

#include <jni.h>

class AntiDetectFix {
public:
    // Apply every on-disk neutralization (memcpy bug, libc kill imports, svc stubs).
    static void patchAll(const char *path);

    // Patch known crash bugs in the guest protection loader after its library is mapped.
    static void onLibraryLoaded(const char *path);

    // Patch the on-disk copy before it is loaded: neutralize the null-source
    // deref in the loader's embedded memcpy small-tail path.
    static void patchFileOnDisk(const char *path);

    // Neutralize the protection's process-kill syscalls in its on-disk copy:
    // kill/tgkill/getpid imports become harmless getpid calls.
    static void patchSyscallsOnDisk(const char *path);

    // Neutralize raw svc stubs for fatal syscalls (exit_group/kill/tgkill/tkill)
    // in the protection's own syscall table: they return 0 instead.
    static void patchSvcStubsOnDisk(const char *path);

    // Install a process-wide seccomp filter that turns fatal syscalls
    // (exit/exit_group/kill/tkill/tgkill) into no-ops returning 0. Catches
    // every termination path the on-disk patterns cannot see (stealth svc
    // trampolines, libc exit(), computed syscall numbers).
    static void installSyscallGuard();
};

#endif //BLACKBOX_ANTIDETECTFIX_H
