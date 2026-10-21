#include <cstring>
#include <cstdint>
#include <cstdlib>
#include <link.h>
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <elf.h>
#include <stddef.h>
#include <sys/prctl.h>
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
    auto insn = reinterpret_cast<uint32_t *>((char *) map + kCrashInsnOff);
    if (insn[0] == kInsnLdrPost && insn[1] == kInsnStrPost) {
        insn[0] = kInsnNop;
        insn[1] = kInsnNop;
        msync(map, mapSize, MS_SYNC);
        ALOGD("memcpy fixup: patched on-disk memcpy tail in %s", path);
    } else if (insn[0] == kInsnNop && insn[1] == kInsnNop) {
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
    // For each fatal nr: "jeq nr -> ret ERRNO(0), else skip". Layout:
    //   ld arch ; jne AARCH64 -> allow ; ld nr ; per nr: jeq+jump over ret ;
    //   ret ALLOW. Offsets below assume exactly this shape.
    constexpr size_t kNrCount = sizeof(kFatal) / sizeof(kFatal[0]);
    struct sock_filter filter[3 + kNrCount * 2 + 1];
    size_t n = 0;
    filter[n++] = BPF_STMT(BPF_LD | BPF_W | BPF_ABS,
                           (uint32_t) offsetof(struct seccomp_data, arch));
    // jt falls through to "ld nr"; jf skips the nr load, every compare and
    // every fatal-ret, landing on the trailing ALLOW.
    filter[n++] = BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_AARCH64,
                           0, (uint8_t) (kNrCount * 2 + 1));
    filter[n++] = BPF_STMT(BPF_LD | BPF_W | BPF_ABS,
                           (uint32_t) offsetof(struct seccomp_data, nr));
    for (size_t i = 0; i < kNrCount; ++i) {
        filter[n++] = BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, kFatal[i], 0, 1);
        filter[n++] = BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | 0);
    }
    filter[n++] = BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW);

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
