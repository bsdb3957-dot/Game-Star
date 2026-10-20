#ifndef BLACKBOX_ANTIDETECTFIX_H
#define BLACKBOX_ANTIDETECTFIX_H

#include <jni.h>

class AntiDetectFix {
public:
    // Patch known crash bugs in the guest protection loader after its library is mapped.
    static void onLibraryLoaded(const char *path);

    // Patch the on-disk copy before it is loaded: neutralize the null-source
    // deref in the loader's embedded memcpy small-tail path.
    static void patchFileOnDisk(const char *path);
};

#endif //BLACKBOX_ANTIDETECTFIX_H
