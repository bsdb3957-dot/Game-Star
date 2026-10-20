#ifndef BLACKBOX_ANTIDETECTFIX_H
#define BLACKBOX_ANTIDETECTFIX_H

#include <jni.h>

class AntiDetectFix {
public:
    // Patch known crash bugs in the guest protection loader after its library is mapped.
    static void onLibraryLoaded(const char *path);
};

#endif //BLACKBOX_ANTIDETECTFIX_H
