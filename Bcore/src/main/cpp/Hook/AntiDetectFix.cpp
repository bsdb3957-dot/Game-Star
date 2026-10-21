#include <cstring>
#include <cstdint>
#include <cstdlib>
#include <link.h>
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <elf.h>
#include <jni.h>
#include <stddef.h>
#include <sys/prctl.h>
#include <signal.h>
#include <ucontext.h>
#include <dlfcn.h>
#include <pthread.h>
#include "JniHook/JniHook.h"
#include <linux/filter.h>
#include <linux/seccomp.h>
#include <linux/audit.h>
#include "AntiDetectFix.h"
#include "Log.h"

extern "C" {
    long avis_noop(long, long, long, long, long, long);
}

__asm__(
        ".text\n"
        ".global avis_noop\n"
        "avis_noop:\n"
#if defined(__aarch64__)
        "mov w0, #0\n"
        "ret\n"
#elif defined(__arm__)
        "mov r0, #0\n"
        "bx lr\n"
#elif defined(__i386__)
        "xor %eax, %eax\n"
        "ret\n"
#endif
        );

// libloader.so ships an inlined memcpy at this offset whose small-tail path
// dereferences a null source when its JNI_OnLoad clears a stack buffer.
static constexpr uintptr_t kEmbeddedMemcpyOff = 0x2c5700;

extern "C" void *avis_memcpy_fixup(void *dst, const void *src, size_t n) {
    if (src == nullptr) {
        ALOGD("memcpy fixup: null src, dst=%p n=%zu, skipped", dst, n);
        return dst;
    }
    return memcpy(dst, src, n);
}

static int phdr_cb(struct dl_phdr_info *info, size_t, void *data) {
    const char *target = static_cast<const char *>(data);
    if (info->dlpi_name == nullptr || strstr(info->dlpi_name, target) == nullptr)
        return 0;

    uintptr_t base = info->dlpi_addr;
    uintptr_t want = base + kEmbeddedMemcpyOff;
    int patched = 0;

    for (int i = 0; i < info->dlpi_phnum; ++i) {
        const ElfW(Phdr) &ph = info->dlpi_phdr[i];
        if (ph.p_type != PT_LOAD || !(ph.p_flags & PF_W))
            continue;
        uintptr_t segStart = base + ph.p_vaddr;
        uintptr_t segEnd = segStart + ph.p_memsz;
        uintptr_t page = (uintptr_t) sysconf(_SC_PAGESIZE);
        for (uintptr_t slot = segStart; slot + sizeof(uintptr_t) <= segEnd; slot += sizeof(uintptr_t)) {
            if (*(uintptr_t *) slot == want) {
                uintptr_t pageStart = slot & ~(page - 1);
                if (mprotect((void *) pageStart, page, PROT_READ | PROT_WRITE) == 0) {
                    *(uintptr_t *) slot = (uintptr_t) &avis_memcpy_fixup;
                    mprotect((void *) pageStart, page, PROT_READ);
                    patched++;
                }
            }
        }
    }
    if (patched > 0) {
        ALOGD("memcpy fixup: patched %d GOT slots in %s", patched, info->dlpi_name);
        return 1;
    }
    return 0;
}

void AntiDetectFix::onLibraryLoaded(const char *path) {
    if (path == nullptr || strstr(path, "libloader.so") == nullptr)
        return;
    dl_iterate_phdr(phdr_cb, (void *) "libloader.so");
}

void AntiDetectFix::patchAll(const char *path) {
    if (path == nullptr)
        return;
    patchFileOnDisk(path);
    patchSyscallsOnDisk(path);
    patchSvcStubsOnDisk(path);
}

// AArch64: ldr x3, [x1], #8  /  str x3, [x6], #8 at the memcpy small-tail path.
// With a null source these fault; the copy result is unused, so nop them out.
static constexpr uintptr_t kCrashInsnOff = 0x2c5768;
static constexpr uint32_t kInsnLdrPost = 0xf8408423;
static constexpr uint32_t kInsnStrPost = 0xf80084c3;
static constexpr uint32_t kInsnNop = 0xd503201f;

void AntiDetectFix::patchFileOnDisk(const char *path) {
    int fd = open(path, O_RDWR);
    if (fd < 0) {
        ALOGD("memcpy fixup: open %s failed", path);
        return;
    }
    struct stat st;
    if (fstat(fd, &st) != 0 || st.st_size < (off_t) (kCrashInsnOff + 8)) {
        close(fd);
        return;
    }
    off_t mapSize = st.st_size;
    void *map = mmap(nullptr, mapSize, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (map == MAP_FAILED) {
        close(fd);
        ALOGD("memcpy fixup: mmap %s failed", path);
        return;
    }
    // The patch offset is a virtual address in the file's address space;
    // convert it through the program headers (vaddr != file offset in this
    // build of the loader).
    const Elf64_Ehdr *eh = reinterpret_cast<const Elf64_Ehdr *>(map);
    off_t crashOff = -1;
    if (eh->e_phoff != 0 && eh->e_phnum != 0) {
        auto ph = reinterpret_cast<const Elf64_Phdr *>((const char *) map + eh->e_phoff);
        for (int i = 0; i < eh->e_phnum; ++i) {
            if (ph[i].p_type == PT_LOAD &&
                kCrashInsnOff >= ph[i].p_vaddr &&
                kCrashInsnOff + 8 <= ph[i].p_vaddr + ph[i].p_filesz) {
                crashOff = (off_t) (ph[i].p_offset + (kCrashInsnOff - ph[i].p_vaddr));
                break;
            }
        }
    }
    if (crashOff < 0 || crashOff + 8 > mapSize) {
        ALOGD("memcpy fixup: vaddr 0x%zx not mapped in %s", (size_t) kCrashInsnOff, path);
        munmap(map, mapSize);
        close(fd);
        return;
    }
    auto insn = reinterpret_cast<uint32_t *>((char *) map + crashOff);
    // A null source faults at every width of the tail (8/4/2/1-byte chunks),
    // not just the first ldr. NOP the whole tail block and force the entry
    // branch to skip it: tbz w2,#3 -> b (unconditional) past the tail.
    // Layout: 0x...64 tbz w2,#3,tail ; tail: 4 ldr/str pairs ; 0x...94 ret.
    static constexpr uint32_t kInsnTbz3 = 0x36180062;   // tbz w2,#3,+0xc
    static constexpr uint32_t kInsnBSkip = 0x1400000c;  // b +0x30 (to ret)
    if (insn[0] == kInsnLdrPost && insn[1] == kInsnStrPost) {
        for (int i = 0; i < 8; ++i)
            insn[i] = kInsnNop;
        insn[-1] = kInsnBSkip;  // replaces the tbz that entered the tail
        msync(map, mapSize, MS_SYNC);
        ALOGD("memcpy fixup: patched on-disk memcpy tail in %s", path);
    } else if (insn[0] == kInsnNop && insn[7] == kInsnNop && insn[-1] == kInsnBSkip) {
        ALOGD("memcpy fixup: %s already patched", path);
    } else {
        ALOGD("memcpy fixup: unexpected insns %08x %08x in %s", insn[0], insn[1], path);
    }
    munmap(map, mapSize);
    close(fd);
}

void AntiDetectFix::patchSyscallsOnDisk(const char *path) {
    int fd = open(path, O_RDWR);
    if (fd < 0)
        return;
    struct stat st;
    if (fstat(fd, &st) != 0) {
        close(fd);
        return;
    }
    off_t mapSize = st.st_size;
    auto base = reinterpret_cast<char *>(mmap(nullptr, mapSize, PROT_READ | PROT_WRITE,
                                              MAP_SHARED, fd, 0));
    if (base == MAP_FAILED) {
        close(fd);
        return;
    }
    auto ehdr = reinterpret_cast<Elf64_Ehdr *>(base);
    Elf64_Dyn *dyn = nullptr;
    for (int i = 0; i < ehdr->e_phnum; ++i) {
        auto ph = reinterpret_cast<Elf64_Phdr *>(base + ehdr->e_phoff +
                                                 i * ehdr->e_phentsize);
        if (ph->p_type == PT_DYNAMIC) {
            dyn = reinterpret_cast<Elf64_Dyn *>(base + ph->p_offset);
            break;
        }
    }
    if (dyn == nullptr) {
        munmap(base, mapSize);
        close(fd);
        return;
    }
    Elf64_Rela *jmprel = nullptr;
    size_t pltrelsz = 0;
    const char *strtab = nullptr;
    Elf64_Sym *symtab = nullptr;
    for (Elf64_Dyn *d = dyn; d->d_tag != DT_NULL; ++d) {
        switch (d->d_tag) {
            case DT_JMPREL: jmprel = reinterpret_cast<Elf64_Rela *>(base + d->d_un.d_ptr); break;
            case DT_PLTRELSZ: pltrelsz = d->d_un.d_val; break;
            case DT_STRTAB: strtab = base + d->d_un.d_ptr; break;
            case DT_SYMTAB: symtab = reinterpret_cast<Elf64_Sym *>(base + d->d_un.d_ptr); break;
            default: break;
        }
    }
    int patched = 0;
    if (jmprel != nullptr && pltrelsz > 0 && strtab != nullptr && symtab != nullptr) {
        for (size_t i = 0; i < pltrelsz / sizeof(Elf64_Rela); ++i) {
            uint32_t symIdx = ELF64_R_SYM(jmprel[i].r_info);
            const char *name = strtab + symtab[symIdx].st_name;
            if (strcmp(name, "kill") == 0 || strcmp(name, "tgkill") == 0 ||
                strcmp(name, "syscall") == 0 || strcmp(name, "abort") == 0) {
                auto slot = reinterpret_cast<void **>(base + jmprel[i].r_offset);
                void *replacement = strcmp(name, "kill") == 0
                                    ? (void *) &getpid : (void *) &avis_noop;
                if (*slot != replacement) {
                    *slot = replacement;
                    patched++;
                }
            }
        }
    }
    if (patched > 0) {
        msync(base, mapSize, MS_SYNC);
        ALOGD("syscall fixup: neutralized %d kill/syscall/abort imports in %s", patched, path);
    }
    munmap(base, mapSize);
    close(fd);
}

// aarch64 syscall numbers that end or signal the process.
static bool fatalSyscall(uint32_t n) {
    switch (n) {
        case 93:   // exit
        case 94:   // exit_group
        case 129:  // kill
        case 130:  // tkill
        case 131:  // tgkill
            return true;
        default:
            return false;
    }
}

void AntiDetectFix::patchSvcStubsOnDisk(const char *path) {
#if defined(__aarch64__)
    int fd = open(path, O_RDWR);
    if (fd < 0)
        return;
    struct stat st;
    if (fstat(fd, &st) != 0) {
        close(fd);
        return;
    }
    off_t mapSize = st.st_size;
    auto base = reinterpret_cast<char *>(mmap(nullptr, mapSize, PROT_READ | PROT_WRITE,
                                              MAP_SHARED, fd, 0));
    if (base == MAP_FAILED) {
        close(fd);
        return;
    }
    constexpr uint32_t kSvc = 0xd4000001;          // svc #0
    constexpr uint32_t kMovX8ImmMask = 0xFFE0001F; // mov x8, #imm
    constexpr uint32_t kMovX8Imm = 0xD2800008;
    int patched = 0;
    auto words = reinterpret_cast<uint32_t *>(base);
    size_t count = mapSize / sizeof(uint32_t);
    for (size_t i = 1; i + 1 < count; ++i) {
        if (words[i] != kSvc)
            continue;
        if ((words[i - 1] & kMovX8ImmMask) != kMovX8Imm)
            continue;
        uint32_t nr = (words[i - 1] >> 5) & 0xFFFF;
        if (!fatalSyscall(nr) && nr != 96 && nr != 117 && nr != 124)
            continue;
        // Return a plausible live value; never execute the syscall.
        // gettid/set_tid_address/set_robust_list callers expect a tid, others expect 0.
        uint32_t imm = nr == 96 || nr == 117 ? 1000 : 0;
        words[i - 1] = 0x52800000 | ((imm & 0xFFFF) << 5); // mov w0, #imm
        words[i] = 0xd65f03c0;                             // ret
        patched++;
    }
    // NOTE: the generic syscall(nr, ...) wrapper is intentionally left intact.
    // The previous in-place filter could not fit in the original 8 words:
    // it overwrote the svc, clobbered the next stub and encoded wrong cmp
    // immediates, so every syscall() call returned garbage. Fatal numbers
    // through this wrapper are caught by installSyscallGuard() instead.
    if (patched > 0) {
        msync(base, mapSize, MS_SYNC);
        ALOGD("svc fixup: neutralized %d fatal svc stubs in %s", patched, path);
    }
    munmap(base, mapSize);
    close(fd);
#endif
}

void AntiDetectFix::installSyscallGuard() {
#if defined(__aarch64__)
    static bool installed = false;
    if (installed)
        return;

    static const uint32_t kFatal[] = {
            93,  // exit
            94,  // exit_group
            129, // kill
            130, // tkill
            131, // tgkill
    };
    constexpr size_t kNrCount = sizeof(kFatal) / sizeof(kFatal[0]);
    struct sock_filter filter[3 + kNrCount * 2 + 1];
    size_t n = 0;
    auto stmt = [&filter, &n](uint16_t code, uint32_t k) {
        filter[n].code = code;
        filter[n].jt = 0;
        filter[n].jf = 0;
        filter[n].k = k;
        n++;
    };
    auto jump = [&filter, &n](uint16_t code, uint32_t k, uint8_t jt, uint8_t jf) {
        filter[n].code = code;
        filter[n].jt = jt;
        filter[n].jf = jf;
        filter[n].k = k;
        n++;
    };
    // Layout: ld arch ; jeq AARCH64 (else -> ALLOW) ; ld nr ;
    // per fatal nr: jeq nr -> ret ERRNO(0) ; ... ; ret ALLOW.
    stmt(BPF_LD | BPF_W | BPF_ABS, (uint32_t) offsetof(struct seccomp_data, arch));
    // jt falls through to "ld nr"; jf skips the nr load, every compare and
    // every fatal-ret, landing on the trailing ALLOW.
    jump(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_AARCH64, 0,
         (uint8_t) (kNrCount * 2 + 1));
    stmt(BPF_LD | BPF_W | BPF_ABS, (uint32_t) offsetof(struct seccomp_data, nr));
    for (size_t i = 0; i < kNrCount; ++i) {
        jump(BPF_JMP | BPF_JEQ | BPF_K, kFatal[i], 0, 1);
        stmt(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | 0);
    }
    stmt(BPF_RET | BPF_K, SECCOMP_RET_ALLOW);

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) {
        ALOGD("syscall guard: PR_SET_NO_NEW_PRIVS failed");
        return;
    }
    struct sock_fprog prog = {(unsigned short) n, filter};
    if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) == 0) {
        installed = true;
        ALOGD("syscall guard: seccomp filter installed");
    } else {
        ALOGD("syscall guard: PR_SET_SECCOMP failed");
    }
#endif
}

// ---------------------------------------------------------- crash shield

static struct sigaction s_prevSegv;
static struct sigaction s_prevBus;

// Deliberate self-crash gadgets dereference constants, unmapped high
// addresses or canonical garbage; real null derefs in the loader we already
// fix. Only addresses far outside any mapped module are considered
// intentional.
static bool plausibleMapped(uintptr_t addr) {
    Dl_info info{};
    return dladdr(reinterpret_cast<void *>(addr), &info) != 0 &&
           info.dli_fbase != nullptr;
}

static void shieldHandler(int sig, siginfo_t *info, void *ctxRaw) {
    auto ctx = static_cast<ucontext_t *>(ctxRaw);
    uintptr_t fault = reinterpret_cast<uintptr_t>(info->si_addr);
    // 64 KiB covers zero-page and near-null arithmetic without swallowing
    // real low-address bugs forever (each skip re-arms the handler).
    if (fault >= 0x10000 && !plausibleMapped(fault)) {
#if defined(__aarch64__)
        ctx->uc_mcontext.pc += 4;
#elif defined(__arm__)
        ctx->uc_mcontext.arm_pc += 4;
#endif
        ALOGD("crash shield: skipped deliberate crash, pc=%p fault=%p",
              (void *) ctx->uc_mcontext.pc, (void *) fault);
        return;
    }
    struct sigaction *prev = sig == SIGSEGV ? &s_prevSegv : &s_prevBus;
    if (prev->sa_sigaction != nullptr) {
        prev->sa_sigaction(sig, info, ctxRaw);
        return;
    }
    signal(sig, SIG_DFL);
    raise(sig);
}

void AntiDetectFix::installCrashShield() {
    static bool installed = false;
    if (installed)
        return;
    struct sigaction sa{};
    sa.sa_sigaction = &shieldHandler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
    if (sigaction(SIGSEGV, &sa, &s_prevSegv) != 0)
        return;
    sigaction(SIGBUS, &sa, &s_prevBus);
    installed = true;
    ALOGD("crash shield: SIGSEGV/SIGBUS handler installed");
}

// ------------------------------------------------------- global fault swallow

// When the protection decides to die and every syscall path is blocked, the
// last cheap gadget is dereferencing a bad pointer (often null or a small
// constant). We install a swallowing handler for the full actionable fault
// set AFTER the guest loads, so anything registered before us (ART runtime
// chain, debuggerd defaults) is bypassed. The skip loop is bounded: a thread
// that faults more than kMaxSwallows times is parked forever — the rest of
// the process, including the watchdog threads we already froze, never sees
// a process death.

static const int kSwallowSignals[] = {SIGSEGV, SIGBUS, SIGILL, SIGFPE, SIGABRT, SIGTRAP};
static constexpr int kMaxSwallows = 4096;

static void swallowHandler(int sig, siginfo_t *info, void *ctxRaw) {
    auto ctx = static_cast<ucontext_t *>(ctxRaw);
    static _Thread_local int swallows = 0;
    if (++swallows > kMaxSwallows) {
        for (;;) pause();
    }
#if defined(__aarch64__)
    ctx->uc_mcontext.pc += 4;
    uintptr_t pc = ctx->uc_mcontext.pc;
#elif defined(__arm__)
    ctx->uc_mcontext.arm_pc += 4;
    uintptr_t pc = ctx->uc_mcontext.arm_pc;
#else
    uintptr_t pc = 0;
#endif
    if (swallows < 64)
        ALOGD("fault swallow: sig=%d pc=%p fault=%p count=%d", sig, (void *) pc,
              (void *) info->si_addr, swallows);
}

void AntiDetectFix::installGlobalFaultSwallow() {
    struct sigaction sa{};
    sa.sa_sigaction = &swallowHandler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_NODEFER;
    for (int sig : kSwallowSignals) {
        sigaction(sig, &sa, nullptr);
    }
    ALOGD("fault swallow: handlers installed");
}

// ------------------------------------------------------------- stop shield

// SIGSTOP cannot be caught, but the protection's in-process raise of it is
// paired with our frozen watchdog threads, so the practical fallback is a
// hang. SIGCONT against a stopped process resumes it; a small thread pokes
// the process back to life whenever a stop slips through.

static void *contLoop(void *) {
    for (;;) {
        sleep(3);
        kill(getpid(), SIGCONT);
    }
    return nullptr;
}

void AntiDetectFix::installStopShield() {
    static bool installed = false;
    if (installed)
        return;
    pthread_t t;
    if (pthread_create(&t, nullptr, &contLoop, nullptr) == 0) {
        pthread_detach(t);
        installed = true;
    }
}

// ----------------------------------------------------------- java kill shield

// Every process-death primitive reachable from guest Java code is neutered
// at its native entry point. We cannot stack-walk to check the caller, so
// these die globally for the guest process — which owns the process anyway.

HOOK_JNI(void, killProcess, JNIEnv *env, jclass clazz, jint pid) {
    ALOGD("java kill shield: Process.killProcess(%d) swallowed", pid);
}

static void logJavaKill(const char *what) {
    ALOGD("java kill shield: %s swallowed", what);
}

void AntiDetectFix::installJavaKillShield(JNIEnv *env) {
    static bool installed = false;
    if (installed)
        return;
    installed = true;

    // android.os.Process.killProcess(int) — the only JVM-reachable primitive
    // that dies instantly without touching signals or syscalls we control.
    JniHook::HookJniFun(env, "android/os/Process", "killProcess", "(I)V",
                        (void *) new_killProcess, (void **) &orig_killProcess, true);

    // Runtime.exit / Runtime.halt / System.exit all converge on
    // java.lang.Shutdown.exit -> Runtime.halt; hook both Runtime entries.
    static auto exitNoop = [](JNIEnv *, jobject, jint) {
        logJavaKill("Runtime.exit");
    };
    static auto haltNoop = [](JNIEnv *, jobject, jint) {
        logJavaKill("Runtime.halt");
    };
    JniHook::HookJniFun(env, "java/lang/Runtime", "exit", "(I)V",
                        (void *) +exitNoop, nullptr, false);
    JniHook::HookJniFun(env, "java/lang/Runtime", "halt", "(I)V",
                        (void *) +haltNoop, nullptr, false);

    ALOGD("java kill shield: installed");
}

// --------------------------------------------------------- instruction trap

// "b ." — an unconditional branch to itself; the thread spins here forever.
static constexpr uint32_t kSelfBranch = 0x14000000;

// "wfi; b ." — park the thread asleep instead of spinning at 100% cpu.
// sigsuspend with an empty mask: sleeps until a signal arrives, then loops
// back. wfi is privileged on some kernels; sigsuspend is always legal.
//   mov x8, #139 (rt_sigsuspend) ; svc #0 ; b .-8  ->  0xd2801168 0xd4000001 0x17fffffe
static constexpr uint32_t kParkInsn[3] = {0xd2801168, 0xd4000001, 0x17fffffe};

static bool freezeFirstInsn(const char *label, void *addr) {
    if (addr == nullptr)
        return false;
    static const long pageSize = sysconf(_SC_PAGESIZE);
    uintptr_t pageStart = ((uintptr_t) addr) & ~(uintptr_t) (pageSize - 1);
    size_t span = ((uintptr_t) addr - pageStart) + sizeof(kParkInsn);
    if (mprotect((void *) pageStart, span, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        ALOGD("insn trap: mprotect failed for %s@%p", label, addr);
        return false;
    }
    uint32_t *word = (uint32_t *) addr;
    if (word[0] == kParkInsn[0] && word[1] == kParkInsn[1] && word[2] == kParkInsn[2]) {
        mprotect((void *) pageStart, span, PROT_READ | PROT_EXEC);
        return true;
    }
    word[0] = kParkInsn[0];
    word[1] = kParkInsn[1];
    word[2] = kParkInsn[2];
    __builtin___clear_cache((char *) addr, (char *) addr + sizeof(kParkInsn));
    mprotect((void *) pageStart, span, PROT_READ | PROT_EXEC);
    ALOGD("insn trap: froze %s@%p", label, addr);
    return true;
}

// The anonymous-exec mapping backing Dobby's closure bridge (and any other
// generated trampoline in the process) is where the protection's dispatch
// stub ends up. First word of each mapping gets a self-branch.
static void freezeAnonExecPages() {
    FILE *fp = fopen("/proc/self/maps", "r");
    if (fp == nullptr)
        return;
    char line[512];
    int frozen = 0;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        // "start-end rwxp ... " with no path column, or a path that is only
        // a [anon:...] label: executable anonymous memory.
        unsigned long start = 0, end = 0;
        char perms[8] = {0};
        char path[256] = {0};
        int fields = sscanf(line, "%lx-%lx %7s %*s %*s %*s %255[^\n]",
                            &start, &end, perms, path);
        if (fields < 3 || perms[2] != 'x')
            continue;
        // Only pages with no path at all are protection trampolines. Any
        // labeled mapping (dalvik JIT cache, art, ashmem) is live runtime
        // code and freezing its first word wedges the VM.
        if (fields == 4)
            continue;
        if (start == 0 || start >= end)
            continue;
        // Skip libart's own executable pages (its entry points live on the
        // first word of some anon mappings; freezing them wedges the VM).
        void *self = (void *) &freezeAnonExecPages;
        Dl_info info{};
        if (dladdr(self, &info) != 0 && info.dli_fbase != nullptr) {
            // libart is loaded as a named module; anything in its address
            // neighbourhood that is not ours belongs to the runtime. Rather
            // than guessing addresses, only freeze pages whose address is
            // above the highest named module — protection trampolines are
            // mmap'd late and land in fresh address space.
            static uintptr_t s_artBase = 0;
            if (s_artBase == 0) {
                void *h = dlopen("libart.so", RTLD_NOW | RTLD_NOLOAD);
                if (h != nullptr) {
                    Dl_info ai{};
                    if (dladdr(h, &ai) != 0)
                        s_artBase = (uintptr_t) ai.dli_fbase;
                }
            }
            if (s_artBase != 0 && start < s_artBase + 0x40000000UL)
                continue;
        }
        if (freezeFirstInsn("anon-exec", (void *) start))
            frozen++;
    }
    fclose(fp);
    ALOGD("insn trap: froze %d anon-exec mappings", frozen);
}

void AntiDetectFix::installInstructionTrap() {
#if defined(__aarch64__)
    static bool installed = false;
    if (installed)
        return;
    installed = true;

    // libc termination entries: inline PLT branches from the loader bypass
    // GOT patching, but they still land on these instructions. NOTE: we do
    // NOT freeze abort/raise — ART itself calls them for every Java
    // exception and the guest hangs instantly. exit/_exit are only used by
    // process termination, never by the VM's normal flow.
    const char *kLibcTargets[] = {"exit", "_exit", "_Exit"};
    for (const char *name : kLibcTargets) {
        void *addr = dlsym(RTLD_DEFAULT, name);
        freezeFirstInsn(name, addr);
    }

    // android.os.Process.killProcess and the VMRuntime kill entries live in
    // libandroid_runtime/libart as JNI natives; the loader calls them through
    // the Java VM without ever touching our hooks.
    const char *kRtTargets[] = {
            "_ZN7android14android_os_Process_killProcessEP7_JNIEnvP8_jobjecti",
    };
    void *rt = dlopen("libandroid_runtime.so", RTLD_NOW | RTLD_NOLOAD);
    if (rt == nullptr)
        rt = dlopen("libandroid_runtime.so", RTLD_NOW);
    if (rt != nullptr) {
        for (const char *name : kRtTargets)
            freezeFirstInsn("Process_killProcess", dlsym(rt, name));
    }

    // JNI dispatch table: CallVoidMethod / CallObjectMethodA etc. get the
    // guest's kill by reflection. Trapping CallVoidMethod catches the common
    // "Process.killProcess(pid)" reflection path — legitimate callers in the
    // box use direct calls, never JNI, so freezing here is safe for us.
    // NOTE: we do NOT trap the whole JNI table (the VM itself needs it); we
    // trap FatalError only, which the protection also uses to die loudly.
    // (CallVoidMethod stays live: the box framework calls into guest code.)
    void *fatal = dlsym(RTLD_DEFAULT, "_ZN7_JNIEnv10FatalErrorEPKc");
    if (fatal != nullptr)
        freezeFirstInsn("JNI FatalError", fatal);

    // NOTE: freezeAnonExecPages() is intentionally not called. Empirically the
    // only unlabeled anon-exec page present at load time is libart's own
    // oat/trampoline region (0x744293c000), and freezing its first word hangs
    // the guest before the first frame. The protection's real kill paths are
    // already covered: syscalls (seccomp), signals (fault swallow), libc exit
    // entries (frozen above), Java-level kills (no-op'd). If Appdome maps a
    // fresh trampoline AFTER this point, it still cannot kill: every way out
    // of that code ends in a frozen or swallowed primitive.
#endif
}
