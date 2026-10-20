#pragma once

#include <sys/types.h>
#include <stddef.h>
#include <unistd.h>
#include <sys/mman.h>
#include "pac_kit.h"

#include "PlatformUnifiedInterface/platform.h"
#include "pac_kit.h"

#if defined(__arm64e__) && __has_feature(ptrauth_calls)
#include <ptrauth.h>
#endif

namespace features {

template <typename T> inline T arm_thumb_fix_addr(T &addr) {
#if defined(__arm__) || defined(__aarch64__)
  addr = (T)((uintptr_t)addr & ~1);
#endif
  return addr;
}

namespace apple {
template <typename T> inline T arm64e_pac_strip(T &addr) {
  return pac_strip(addr);
}

template <typename T> inline T arm64e_pac_sign(T &addr) {
  return pac_sign(addr);
}

template <typename T> inline T arm64e_pac_strip_and_sign(T &addr) {
  return pac_strip_and_sign(addr);
}
} // namespace apple

namespace android {
inline void make_memory_readable(void *address, size_t size) {
#if defined(ANDROID)
  long pageSize = sysconf(_SC_PAGESIZE);
  if (pageSize > 0) {
    auto page = (void *)ALIGN_FLOOR(address, pageSize);
    mprotect(page, (size_t) pageSize, PROT_READ | PROT_EXEC);
  }
#endif
}
} // namespace android
} // namespace features