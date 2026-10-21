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

// AArch64: ldr x3, [x1], #8  /  str x3, [x6], #8 at the memcpy small-tail path.
// With a null source these fault; the copy result is unused, so nop them out.
// This build has THREE identical tail blocks (the compiler outlined the same
// memcpy epilogue at each copy site); the tombstone named 0x2c5768, but the
// other two are equally lethal.
static constexpr uintptr_t kCrashInsnOffs[] = {0x2b276c, 0x2b2728, 0x2c5768};
static constexpr uintptr_t kCrashInsnOff = kCrashInsnOffs[2];
static constexpr uint32_t kInsnLdrPost = 0xf8408423;
static constexpr uint32_t kInsnStrPost = 0xf80084c3;
static constexpr uint32_t kInsnNop = 0xd503201f;
static constexpr uint32_t kInsnBSkip = 0x1400000c;  // b +0x30 (to ret)



// Runtime mirror of patchFileOnDisk. Exported so RuntimeHook can call it
// BEFORE dlopen: patchFileOnDisk already NOP'd the tail on disk, so the
// kernel maps a clean image — this re-applies the fix if anything (e.g. a
// stale copy) slipped through, and is a no-op on an already-clean image.
void AntiDetectFix::neutralizeLiveTail() {
    dl_iterate_phdr([](struct dl_phdr_info *info, size_t, void *) -> int {
        if (info->dlpi_name == nullptr || strstr(info->dlpi_name, "libloader.so") == nullptr)
            return 0;
        static const long pageSize = sysconf(_SC_PAGESIZE);
        for (uintptr_t off : kCrashInsnOffs) {
            uintptr_t addr = info->dlpi_addr + off;
            uintptr_t pageStart = addr & ~(uintptr_t) (pageSize - 1);
            size_t span = (addr - pageStart) + 0x34;  // tbz at -4 .. end of tail
            if (mprotect((void *) pageStart, span, PROT_READ | PROT_WRITE | PROT_EXEC) != 0)
                continue;
            auto insn = reinterpret_cast<uint32_t *>(addr);
            if (insn[0] == kInsnLdrPost && insn[1] == kInsnStrPost) {
                for (int i = 0; i < 8; ++i)
                    insn[i] = kInsnNop;
                insn[-1] = kInsnBSkip;
                __builtin___clear_cache((char *) &insn[-1], (char *) &insn[8]);
                ALOGD("memcpy fixup: neutralized memcpy tail in live image @0x%zx", off);
            }
            mprotect((void *) pageStart, span, PROT_READ | PROT_EXEC);
        }
        return 1;
    }, nullptr);
}

void AntiDetectFix::onLibraryLoaded(const char *path) {
    if (path == nullptr || strstr(path, "libloader.so") == nullptr)
        return;
    dl_iterate_phdr(phdr_cb, (void *) "libloader.so");
    neutralizeLiveTail();
}

void AntiDetectFix::patchAll(const char *path) {
    if (path == nullptr)
        return;
    patchFileOnDisk(path);
    patchSyscallsOnDisk(path);
    patchSvcStubsOnDisk(path);
}


void AntiDetectFix::patchFileOnDisk(const char *path) {
    int fd = open(path, O_RDWR);
    if (fd < 0) {
        ALOGD("memcpy fixup: open %s failed", path);
        return;
    }
    struct stat st;
    if (fstat(fd, &st) != 0 || st.st_size < (off_t) (kCrashInsnOffs[2] + 8)) {
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
    // The patch offsets are virtual addresses; convert each through the
    // program headers (vaddr != file offset in general).
    const Elf64_Ehdr *eh = reinterpret_cast<const Elf64_Ehdr *>(map);
    auto v2o = [&](uintptr_t vaddr) -> off_t {
        if (eh->e_phoff == 0 || eh->e_phnum == 0)
            return -1;
        auto ph = reinterpret_cast<const Elf64_Phdr *>((const char *) map + eh->e_phoff);
        for (int i = 0; i < eh->e_phnum; ++i) {
            if (ph[i].p_type == PT_LOAD &&
                vaddr >= ph[i].p_vaddr &&
                vaddr + 8 <= ph[i].p_vaddr + ph[i].p_filesz)
                return (off_t) (ph[i].p_offset + (vaddr - ph[i].p_vaddr));
        }
        return -1;
    };
    int patched = 0, already = 0;
    for (uintptr_t off : kCrashInsnOffs) {
        off_t crashOff = v2o(off);
        if (crashOff < 0 || crashOff + 8 > mapSize)
            continue;
        auto insn = reinterpret_cast<uint32_t *>((char *) map + crashOff);
        // A null source faults at every width of the tail (8/4/2/1-byte
        // chunks), not just the first ldr. NOP the whole tail block and force
        // the entry branch to skip it.
        if (insn[0] == kInsnLdrPost && insn[1] == kInsnStrPost) {
            for (int i = 0; i < 8; ++i)
                insn[i] = kInsnNop;
            insn[-1] = kInsnBSkip;
            patched++;
        } else if (insn[0] == kInsnNop && insn[7] == kInsnNop && insn[-1] == kInsnBSkip) {
            already++;
        } else {
            ALOGD("memcpy fixup: unexpected insns %08x %08x @0x%zx in %s",
                  insn[0], insn[1], off, path);
        }
    }
    if (patched > 0) {
        msync(map, mapSize, MS_SYNC);
        ALOGD("memcpy fixup: patched %d on-disk memcpy tails in %s", patched, path);
    } else if (already > 0) {
        ALOGD("memcpy fixup: %s already patched (%d sites)", path, already);
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
