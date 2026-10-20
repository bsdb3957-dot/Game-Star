#include <cstring>
#include <cstdint>
#include <cstdlib>
#include <link.h>
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <elf.h>
#include "AntiDetectFix.h"
#include "Log.h"

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
            if (strcmp(name, "kill") == 0 || strcmp(name, "tgkill") == 0) {
                auto slot = reinterpret_cast<void **>(base + jmprel[i].r_offset);
                if (*slot != (void *) &getpid) {
                    *slot = (void *) &getpid;
                    patched++;
                }
            }
        }
    }
    if (patched > 0) {
        msync(base, mapSize, MS_SYNC);
        ALOGD("syscall fixup: neutralized %d kill/getpid imports in %s", patched, path);
    }
    munmap(base, mapSize);
    close(fd);
}
