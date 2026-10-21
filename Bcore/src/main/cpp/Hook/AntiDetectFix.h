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

    // Last line of defense: raise a SIGSEGV handler that consumes faults in
    // the protection loader's intentional-crash path (its "die on detect"
    // gadget dereferences a garbage pointer). Crashes whose fault address is
    // far outside any mapped module — i.e. obviously intentional — resume at
    // the next instruction; everything else chains to the default handler.
    static void installCrashShield();

    // Process-wide insurance: disable every actionable fatal signal handler
    // (ART's runtime chain, debuggerd defaults) so the protection's
    // deliberate-crash gadgets hit our swallowing handler instead. Skips a
    // configurable set of signals we must never eat.
    static void installGlobalFaultSwallow();

    // Break the protection's "die by pausing main" fallback: SIGSTOP raised
    // in-process is observed and immediately continued.
    static void installStopShield();

    // Pin the JVM runtime in place: installs Dobby hooks on the native entry
    // points Appdome uses to make the process die from Java code
    // (Process.killProcess, Runtime.exit/halt, System.exit, VMRuntime.kill).
    // They log the caller and return as if nothing happened.
    static void installJavaKillShield(JNIEnv *env);
};

#endif //BLACKBOX_ANTIDETECTFIX_H
