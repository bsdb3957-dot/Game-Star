#include "dobby/dobby_internal.h"
#include "InterceptRouting/InlineHookRouting.h"

// Force emission of the inline DobbyHook defined in InlineHookRouting.h.
extern "C" int dobby_hook_force_ref(void *a, void *f, void **o) {
    return DobbyHook(a, f, o);
}
