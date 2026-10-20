#include "TrampolineBridge/ClosureTrampolineBridge/common_bridge_handler.h"

// Force emission: the header defines common_closure_bridge_handler as
// extern "C" inline, which produces no symbol on its own.
extern "C" void (*force_common_closure_bridge_handler_ref)(DobbyRegisterContext *, ClosureTrampoline *) =
        &common_closure_bridge_handler;
