#ifndef BLACKBOX_ANTIDETECTFIX_H
#define BLACKBOX_ANTIDETECTFIX_H

#include <jni.h>

class AntiDetectFix {
public:
    // Apply every on-disk neutralization (memcpy bug, libc kill imports, svc stubs).
    static void patchAll(const char *path);

    // Patch the guest protection loader's GOT after its library is mapped.
    static void onLibraryLoaded(const char *path);

    // NOP the memcpy tail in the already-mapped image (no-op when not mapped
    // or already clean).
    static void neutralizeLiveTail();

    // Patch the on-disk copy before it is loaded: neutralize the null-source
    // deref in the loader's embedded memcpy small-tail path.
    static void patchFileOnDisk(const char *path);

    // Neutralize the protection's process-kill syscalls in its on-disk copy.
    static void patchSyscallsOnDisk(const char *path);

    // Neutralize raw svc stubs for fatal syscalls in the protection's own
    // syscall table: they return 0 instead.
    static void patchSvcStubsOnDisk(const char *path);
};

#endif //BLACKBOX_ANTIDETECTFIX_H
