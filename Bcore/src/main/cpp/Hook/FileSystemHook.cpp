//
// GOT/PLT hook layer: replaces libc entries in every loaded module of the
// guest process so protection code (libloader.so) cannot observe the host
// box through /proc content, package paths or our container library.
//

#include "FileSystemHook.h"

#include <dirent.h>
#include <dlfcn.h>
#include <elf.h>
#include <fcntl.h>
#include <link.h>
#include <netdb.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <sys/utsname.h>
#include <unistd.h>

#include "Log.h"

// The host package must never appear in strings the guest protection can
// observe; it is rewritten to a plausible standalone install of the guest.
static const char kHostPkg[] = "com.avis.tool";
static const char kGuestPkg[] = "com.catsbit.oxidesurvivalisland";
static const size_t kHostPkgLen = sizeof(kHostPkg) - 1;
static const size_t kGuestPkgLen = sizeof(kGuestPkg) - 1;
// Our container library: dropped from /proc maps-like content, and modules
// named after it are excluded from patching (their slots must stay real).
static const char kBridgeLib[] = "libABridge.so";
static const size_t kBridgeLibLen = sizeof(kBridgeLib) - 1;

// ---------------------------------------------------------------- utils

static const char *memFind(const char *hay, size_t hayLen,
                           const char *needle, size_t needleLen) {
    if (needleLen == 0 || hayLen < needleLen) return nullptr;
    for (size_t i = 0; i + needleLen <= hayLen; ++i) {
        if (hay[i] == needle[0] && memcmp(hay + i, needle, needleLen) == 0)
            return hay + i;
    }
    return nullptr;
}

// Replace every occurrence of from with to inside a string, shifting the
// tail right. Returns the new length, or -1 when the result would exceed
// maxLen (buffer left untouched then).
static long replaceAllInPlace(char *buf, size_t len, size_t maxLen,
                              const char *from, size_t fromLen,
                              const char *to, size_t toLen, bool hasTerminator) {
    size_t count = 0;
    const char *p = buf;
    const char *end = buf + len;
    while ((p = memFind(p, (size_t) (end - p), from, fromLen)) != nullptr) {
        count++;
        p += fromLen;
    }
    if (count == 0) return (long) len;
    size_t extra = (toLen - fromLen) * count;
    if (len + extra > maxLen) return -1;

    size_t pos = 0;
    char *hit;
    while ((hit = (char *) memFind(buf + pos, len - pos, from, fromLen)) != nullptr) {
        size_t off = (size_t) (hit - buf);
        size_t tailLen = len - off - fromLen + (hasTerminator ? 1 : 0);
        memmove(buf + off + toLen, buf + off + fromLen, tailLen);
        memcpy(buf + off, to, toLen);
        len += toLen - fromLen;
        pos = off + toLen;
    }
    return (long) len;
}

// /proc text (maps, cmdline, status): drop lines naming our container
// library, rewrite the host package elsewhere. Grows are capped so the
// caller's buffer is never overrun; occurrences that cannot fit stay as-is.
static size_t filterProcBuffer(char *buf, size_t n, size_t cap) {
    size_t out = 0, i = 0;
    while (i < n) {
        const char *nl = (const char *) memchr(buf + i, '\n', n - i);
        size_t end = nl != nullptr ? (size_t) (nl - buf) + 1 : n;
        size_t len = end - i;
        if (memFind(buf + i, len, kBridgeLib, kBridgeLibLen) == nullptr) {
            if (out != i) memmove(buf + out, buf + i, len);
            out += len;
        }
        i = end;
    }

    size_t pos = 0;
    for (;;) {
        char *hit = (char *) memFind(buf + pos, out - pos, kHostPkg, kHostPkgLen);
        if (hit == nullptr) break;
        size_t off = (size_t) (hit - buf);
        size_t tailLen = out - off - kHostPkgLen;
        if (off + kGuestPkgLen + tailLen > cap) {
            pos = off + kHostPkgLen;
            continue;
        }
        memmove(buf + off + kGuestPkgLen, buf + off + kHostPkgLen, tailLen);
        memcpy(buf + off, kGuestPkg, kGuestPkgLen);
        out += kGuestPkgLen - kHostPkgLen;
        pos = off + kGuestPkgLen;
    }
    return out;
}

// --------------------------------------------------- stdio stream tracking

// Only /proc-originated streams are filtered; their content is text we can
// safely rewrite, unlike arbitrary stdio traffic (assets, dex, ...).
struct StreamInfo {
    FILE *fp;
    bool proc;
};

static StreamInfo s_streams[128];
static size_t s_streamCount;
static pthread_mutex_t s_streamLock = PTHREAD_MUTEX_INITIALIZER;

static void noteStream(FILE *fp, bool proc) {
    if (fp == nullptr) return;
    pthread_mutex_lock(&s_streamLock);
    for (size_t i = 0; i < s_streamCount; ++i) {
        if (s_streams[i].fp == fp) {
            s_streams[i].proc = proc;
            pthread_mutex_unlock(&s_streamLock);
            return;
        }
    }
    if (s_streamCount < sizeof(s_streams) / sizeof(s_streams[0])) {
        s_streams[s_streamCount].fp = fp;
        s_streams[s_streamCount].proc = proc;
        s_streamCount++;
    }
    pthread_mutex_unlock(&s_streamLock);
}

static bool isProcStream(FILE *fp) {
    pthread_mutex_lock(&s_streamLock);
    for (size_t i = 0; i < s_streamCount; ++i) {
        if (s_streams[i].fp == fp) {
            bool cached = s_streams[i].proc;
            pthread_mutex_unlock(&s_streamLock);
            return cached;
        }
    }
    pthread_mutex_unlock(&s_streamLock);

    // Streams opened outside fopen/popen (fdopen, inherited FILEs) are
    // classified lazily via their fd symlink.
    bool proc = false;
    int fd = fileno(fp);
    if (fd >= 0) {
        char link[64];
        char target[512];
        snprintf(link, sizeof(link), "/proc/self/fd/%d", fd);
        ssize_t n = readlink(link, target, sizeof(target) - 1);
        if (n > 0) {
            target[n] = '\0';
            proc = strncmp(target, "/proc/", 6) == 0;
        }
    }
    noteStream(fp, proc);
    return proc;
}

// ------------------------------------------------------------ replacements

static int (*o_open)(const char *, int, ...);
static int (*o_openat)(int, const char *, int, ...);
static int (*o_stat)(const char *, struct stat *);
static int (*o_lstat)(const char *, struct stat *);
static int (*o_fstatat)(int, const char *, struct stat *, int);
static int (*o_access)(const char *, int);
static int (*o_faccessat)(int, const char *, int, int);
static ssize_t (*o_readlink)(const char *, char *, size_t);
static ssize_t (*o_readlinkat)(int, const char *, char *, size_t);
static DIR *(*o_opendir)(const char *);
static FILE *(*o_fopen)(const char *, const char *);
static FILE *(*o_popen)(const char *, const char *);
static int (*o_system)(const char *);
static int (*o_uname)(struct utsname *);
static int (*o_connect)(int, const struct sockaddr *, socklen_t);
static int (*o_getaddrinfo)(const char *, const char *, const struct addrinfo *,
                            struct addrinfo **);
static int (*o_system_property_get)(const char *, char *);
static char *(*o_fgets)(char *, int, FILE *);
static size_t (*o_fread)(void *, size_t, size_t, FILE *);

static char *fetchLine(char *s, int size, FILE *stream) {
    return o_fgets != nullptr ? o_fgets(s, size, stream) : fgets(s, size, stream);
}

static size_t readBytes(void *ptr, size_t size, size_t nmemb, FILE *stream) {
    return o_fread != nullptr ? o_fread(ptr, size, nmemb, stream)
                              : fread(ptr, size, nmemb, stream);
}

// Path-taking calls never echo the path back to the caller, so they run on
// the original path: guest IO keeps working and only string-returning
// surfaces (readlink, fgets, fread) need rewriting.

static int r_open(const char *path, int flags, ...) {
    va_list ap;
    va_start(ap, flags);
    mode_t mode = (flags & O_CREAT) ? (mode_t) va_arg(ap, int) : 0;
    va_end(ap);
    return o_open != nullptr ? o_open(path, flags, mode) : open(path, flags, mode);
}

static int r_openat(int dirfd, const char *path, int flags, ...) {
    va_list ap;
    va_start(ap, flags);
    mode_t mode = (flags & O_CREAT) ? (mode_t) va_arg(ap, int) : 0;
    va_end(ap);
    return o_openat != nullptr ? o_openat(dirfd, path, flags, mode)
                               : openat(dirfd, path, flags, mode);
}

static int r_stat(const char *path, struct stat *buf) {
    return o_stat != nullptr ? o_stat(path, buf) : stat(path, buf);
}

static int r_lstat(const char *path, struct stat *buf) {
    return o_lstat != nullptr ? o_lstat(path, buf) : lstat(path, buf);
}

static int r_fstatat(int dirfd, const char *path, struct stat *buf, int flags) {
    return o_fstatat != nullptr ? o_fstatat(dirfd, path, buf, flags)
                                : fstatat(dirfd, path, buf, flags);
}

static int r_access(const char *path, int mode) {
    return o_access != nullptr ? o_access(path, mode) : access(path, mode);
}

static int r_faccessat(int dirfd, const char *path, int mode, int flags) {
    return o_faccessat != nullptr ? o_faccessat(dirfd, path, mode, flags)
                                  : faccessat(dirfd, path, mode, flags);
}

// readlink results are caller-visible strings: rewrite the host package to
// the standalone guest install, fixing the length. When the rewrite does
// not fit the caller's buffer, return the plausible standalone prefix and
// drop the rest rather than leak the host name in truncated output.
static ssize_t rewriteReadlinkResult(char *buf, ssize_t n, size_t bufsiz) {
    if (n <= 0 || buf == nullptr) return n;
    size_t len = (size_t) n;
    size_t pos = 0;
    while (pos < len) {
        const char *hit = memFind(buf + pos, len - pos, kHostPkg, kHostPkgLen);
        if (hit == nullptr) break;
        size_t off = (size_t) (hit - buf);
        size_t tailLen = len - off - kHostPkgLen;
        if (off + kGuestPkgLen + tailLen <= bufsiz) {
            memmove(buf + off + kGuestPkgLen, buf + off + kHostPkgLen, tailLen);
            memcpy(buf + off, kGuestPkg, kGuestPkgLen);
            len += kGuestPkgLen - kHostPkgLen;
            pos = off + kGuestPkgLen;
            continue;
        }
        if (off + kGuestPkgLen <= bufsiz) {
            memcpy(buf + off, kGuestPkg, kGuestPkgLen);
            return (ssize_t) (off + kGuestPkgLen);
        }
        memcpy(buf + off, kGuestPkg, bufsiz - off);
        return (ssize_t) bufsiz;
    }
    return (ssize_t) len;
}

static ssize_t r_readlink(const char *path, char *buf, size_t bufsiz) {
    ssize_t n = o_readlink != nullptr ? o_readlink(path, buf, bufsiz)
                                      : readlink(path, buf, bufsiz);
    return rewriteReadlinkResult(buf, n, bufsiz);
}

static ssize_t r_readlinkat(int dirfd, const char *path, char *buf, size_t bufsiz) {
    ssize_t n = o_readlinkat != nullptr ? o_readlinkat(dirfd, path, buf, bufsiz)
                                        : readlinkat(dirfd, path, buf, bufsiz);
    return rewriteReadlinkResult(buf, n, bufsiz);
}

static DIR *r_opendir(const char *path) {
    return o_opendir != nullptr ? o_opendir(path) : opendir(path);
}

static FILE *r_fopen(const char *path, const char *mode) {
    FILE *fp = o_fopen != nullptr ? o_fopen(path, mode) : fopen(path, mode);
    if (fp != nullptr && path != nullptr && strncmp(path, "/proc/", 6) == 0)
        noteStream(fp, true);
    return fp;
}

static FILE *r_popen(const char *command, const char *type) {
    FILE *fp = o_popen != nullptr ? o_popen(command, type) : popen(command, type);
    if (fp != nullptr && command != nullptr && strstr(command, "/proc/") != nullptr)
        noteStream(fp, true);
    return fp;
}

static char *r_fgets(char *s, int size, FILE *stream) {
    char *line = fetchLine(s, size, stream);
    if (line == nullptr || size <= 0) return line;
    if (!isProcStream(stream)) return line;

    for (int dropped = 0; dropped < 512; ++dropped) {
        size_t len = strlen(line);
        if (memFind(line, len, kBridgeLib, kBridgeLibLen) != nullptr) {
            line = fetchLine(s, size, stream);
            if (line == nullptr) return nullptr;
            continue;
        }
        if (memFind(line, len, kHostPkg, kHostPkgLen) != nullptr) {
            long newLen = replaceAllInPlace(line, len, (size_t) size - 1,
                                            kHostPkg, kHostPkgLen,
                                            kGuestPkg, kGuestPkgLen, true);
            if (newLen < 0) {
                // Would not fit: drop the line instead of leaking.
                line = fetchLine(s, size, stream);
                if (line == nullptr) return nullptr;
                continue;
            }
        }
        break;
    }
    return line;
}

static size_t r_fread(void *ptr, size_t size, size_t nmemb, FILE *stream) {
    size_t n = readBytes(ptr, size, nmemb, stream);
    if (n == 0 || ptr == nullptr || size == 0) return n;
    if (!isProcStream(stream)) return n;
    size_t bytes = filterProcBuffer((char *) ptr, n * size, size * nmemb);
    return bytes / size;
}

// Pass-through hooks kept for parity with the observed protection surface.

static int r_system(const char *command) {
    return o_system != nullptr ? o_system(command) : system(command);
}

static int r_uname(struct utsname *buf) {
    return o_uname != nullptr ? o_uname(buf) : uname(buf);
}

static int r_connect(int fd, const struct sockaddr *addr, socklen_t len) {
    return o_connect != nullptr ? o_connect(fd, addr, len) : connect(fd, addr, len);
}

static int r_getaddrinfo(const char *node, const char *service,
                         const struct addrinfo *hints, struct addrinfo **res) {
    return o_getaddrinfo != nullptr
           ? o_getaddrinfo(node, service, hints, res)
           : getaddrinfo(node, service, hints, res);
}

static int r_system_property_get(const char *name, char *value) {
    return o_system_property_get != nullptr
           ? o_system_property_get(name, value)
           : __system_property_get(name, value);
}

struct GotHook {
    const char *symbol;
    void *replacement;
    void **original;
};

static GotHook s_hooks[] = {
        {"open",                  (void *) &r_open,                (void **) &o_open},
        {"openat",                (void *) &r_openat,              (void **) &o_openat},
        {"stat",                  (void *) &r_stat,                (void **) &o_stat},
        {"stat64",                (void *) &r_stat,                (void **) &o_stat},
        {"lstat",                 (void *) &r_lstat,               (void **) &o_lstat},
        {"lstat64",               (void *) &r_lstat,               (void **) &o_lstat},
        {"fstatat",               (void *) &r_fstatat,             (void **) &o_fstatat},
        {"fstatat64",             (void *) &r_fstatat,             (void **) &o_fstatat},
        {"access",                (void *) &r_access,              (void **) &o_access},
        {"faccessat",             (void *) &r_faccessat,           (void **) &o_faccessat},
        {"readlink",              (void *) &r_readlink,            (void **) &o_readlink},
        {"readlinkat",            (void *) &r_readlinkat,          (void **) &o_readlinkat},
        {"opendir",               (void *) &r_opendir,             (void **) &o_opendir},
        {"fopen",                 (void *) &r_fopen,               (void **) &o_fopen},
        {"fgets",                 (void *) &r_fgets,               (void **) &o_fgets},
        {"fread",                 (void *) &r_fread,               (void **) &o_fread},
        {"popen",                 (void *) &r_popen,               (void **) &o_popen},
        {"system",                (void *) &r_system,              (void **) &o_system},
        {"uname",                 (void *) &r_uname,               (void **) &o_uname},
        {"connect",               (void *) &r_connect,             (void **) &o_connect},
        {"getaddrinfo",           (void *) &r_getaddrinfo,         (void **) &o_getaddrinfo},
        {"__system_property_get", (void *) &r_system_property_get, (void **) &o_system_property_get},
};

// ------------------------------------------------------------ got patching

#if defined(__LP64__)
#define GOT_R_TYPE(info) ELF64_R_TYPE(info)
#define GOT_R_SYM(info) ELF64_R_SYM(info)
#else
#define GOT_R_TYPE(info) ELF32_R_TYPE(info)
#define GOT_R_SYM(info) ELF32_R_SYM(info)
#endif

static bool hookableRelocType(uint32_t type) {
#if defined(__aarch64__)
    return type == R_AARCH64_JUMP_SLOT || type == R_AARCH64_GLOB_DAT;
#elif defined(__arm__)
    return type == R_ARM_JUMP_SLOT || type == R_ARM_GLOB_DAT;
#elif defined(__x86_64__)
    return type == R_X86_64_JUMP_SLOT || type == R_X86_64_GLOB_DAT;
#elif defined(__i386__)
    return type == R_386_JMP_SLOT || type == R_386_GLOB_DAT;
#else
    (void) type;
    return false;
#endif
}

// Dynamic entries hold link-time vaddres or already-relocated pointers;
// anything below the load bias is still link-time.
static uintptr_t dynPtr(ElfW(Addr) value, uintptr_t base) {
    if (value < (ElfW(Addr)) base) return base + (uintptr_t) value;
    return (uintptr_t) value;
}

static void patchSlot(void **slot, void *replacement) {
    static const long pageSize = sysconf(_SC_PAGESIZE);
    uintptr_t pageStart = ((uintptr_t) slot) & ~(uintptr_t) (pageSize - 1);
    if (mprotect((void *) pageStart, (size_t) pageSize, PROT_READ | PROT_WRITE) != 0)
        return;
    *slot = replacement;
    mprotect((void *) pageStart, (size_t) pageSize, PROT_READ);
}

static int tryPatchSlot(const GotHook &hook, void **slot) {
    void *cur = *slot;
    if (cur == nullptr) return 0;           // weak or undefined import
    if (cur == hook.replacement) return 0;  // already ours
    if (*hook.original == nullptr) *hook.original = cur;
    patchSlot(slot, hook.replacement);
    return 1;
}

template<typename RelT>
static int patchRelocs(uintptr_t base, uintptr_t relocs, size_t sizeBytes,
                       uintptr_t symtab, uintptr_t strtab) {
    if (relocs == 0 || sizeBytes == 0) return 0;
    int patched = 0;
    const RelT *end = (const RelT *) (relocs + sizeBytes);
    for (const RelT *r = (const RelT *) relocs; r + 1 <= end; ++r) {
        if (!hookableRelocType((uint32_t) GOT_R_TYPE(r->r_info))) continue;
        size_t symIdx = (size_t) GOT_R_SYM(r->r_info);
        const char *name =
                (const char *) (strtab + ((const ElfW(Sym) *) symtab)[symIdx].st_name);
        for (size_t i = 0; i < sizeof(s_hooks) / sizeof(s_hooks[0]); ++i) {
            if (strcmp(name, s_hooks[i].symbol) == 0 &&
                tryPatchSlot(s_hooks[i], (void **) (base + r->r_offset))) {
                patched++;
                break;
            }
        }
    }
    return patched;
}

static int patchGot(uintptr_t base, const ElfW(Dyn) *dyn, size_t dynCount) {
    if (dyn == nullptr || dynCount == 0) return 0;

    uintptr_t strtab = 0, symtab = 0;
    uintptr_t jmprel = 0, rela = 0, rel = 0;
    size_t jmprelSz = 0, relaSz = 0, relSz = 0;
    bool jmprelIsRela = false;

    for (size_t i = 0; i < dynCount; ++i) {
        const ElfW(Dyn) &d = dyn[i];
        switch (d.d_tag) {
            case DT_NULL: goto done;
            case DT_STRTAB: strtab = dynPtr(d.d_un.d_ptr, base); break;
            case DT_SYMTAB: symtab = dynPtr(d.d_un.d_ptr, base); break;
            case DT_JMPREL: jmprel = dynPtr(d.d_un.d_ptr, base); break;
            case DT_PLTRELSZ: jmprelSz = d.d_un.d_val; break;
            case DT_PLTREL: jmprelIsRela = d.d_un.d_val == DT_RELA; break;
            case DT_RELA: rela = dynPtr(d.d_un.d_ptr, base); break;
            case DT_RELASZ: relaSz = d.d_un.d_val; break;
            case DT_REL: rel = dynPtr(d.d_un.d_ptr, base); break;
            case DT_RELSZ: relSz = d.d_un.d_val; break;
            default: break;
        }
    }
done:
    if (strtab == 0 || symtab == 0) return 0;
    int patched = 0;
    if (jmprel != 0 && jmprelSz != 0) {
        if (jmprelIsRela)
            patched += patchRelocs<ElfW(Rela)>(base, jmprel, jmprelSz, symtab, strtab);
        else
            patched += patchRelocs<ElfW(Rel)>(base, jmprel, jmprelSz, symtab, strtab);
    }
    if (rela != 0 && relaSz != 0)
        patched += patchRelocs<ElfW(Rela)>(base, rela, relaSz, symtab, strtab);
    if (rel != 0 && relSz != 0)
        patched += patchRelocs<ElfW(Rel)>(base, rel, relSz, symtab, strtab);
    return patched;
}

static int phdr_cb(struct dl_phdr_info *info, size_t size, void *data) {
    (void) size;
    (void) data;
    if (info->dlpi_name == nullptr) return 0;
    // The container's own library must keep real libc entries: the
    // replacements call them, and hooking ourselves would recurse.
    if (strstr(info->dlpi_name, kBridgeLib) != nullptr) return 0;

    uintptr_t base = info->dlpi_addr;
    for (int i = 0; i < info->dlpi_phnum; ++i) {
        if (info->dlpi_phdr[i].p_type != PT_DYNAMIC) continue;
        int patched = patchGot(base,
                               (const ElfW(Dyn) *) (base + info->dlpi_phdr[i].p_vaddr),
                               info->dlpi_phdr[i].p_memsz / sizeof(ElfW(Dyn)));
        if (patched > 0) {
            ALOGD("FileSystemHook: patched %d got slots in %s",
                  patched, info->dlpi_name[0] != '\0' ? info->dlpi_name : "(main)");
        }
        break;
    }
    return 0;
}

void FileSystemHook::patchAllModules() {
    dl_iterate_phdr(phdr_cb, nullptr);
}

void FileSystemHook::install(JNIEnv *env) {
    (void) env;
    patchAllModules();
    ALOGD("FileSystemHook: installed");
}
