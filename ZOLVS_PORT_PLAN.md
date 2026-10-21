# ZOLVS Port Plan — как победить Appdome в Avis Tool

Документ описывает точную механику рабочей виртуалки ZOLVS (из анализа
`ZOLVSANTIBANDS.0.0_arm64-v8a-release.apk` → `libblackbox.so`, символы не
стрипнуты, проверено дизассемблированием + эмуляцией Unicorn) и план её
переноса в наш проект вместо текущего брутфорса.

## Что мы делали не так

Наш стек (все попытки победить защиту ПОСЛЕ её срабатывания):

- seccomp-фильтр на exit/kill/tkill/tgkill (`AntiDetectFix::installSyscallGuard`)
- проглатывание SIGSEGV/SIGBUS/SIGILL/SIGFPE/SIGABRT/SIGTRAP (`installGlobalFaultSwallow`)
- self-branch в первую инструкцию libc `exit/_exit/_Exit` (`installInstructionTrap`)
- фриз anon-exec страниц (ломал libart → чёрный экран)
- паркинг watchdog-тредов по `libloader.so` через `pthread_create` (уже есть)
- on-disk патчи `libloader.so`: memcpy-tail (3 сайта), импорты kill/tgkill/syscall/abort, svc-стабы

Итог: процесс либо умирал мимо всех щитов (tombstone: SIGSEGV, null,
`libloader.so+0x2c5768` — Appdome крашится по-своему, переустанавливая свои
хендлеры и перепрыгивая наши), либо наши фризы ломали ART (чёрный экран).
Победить «в лоб» после детекта нельзя — защита всегда найдёт способ умереть.

## Как это делает ZOLVS (верифицировано)

В `libblackbox.so` ZOLVS НЕТ: seccomp, signal-фризов, exit-трапов, on-disk
патчей libloader, анон-фризов. Вместо этого — два точечных механизма:

### 1. AppdomeJniTrap (`Hook/AppdomeJniTrap.cpp` в их дереве)

Символы: `InstallAppdomeJniTrap` (0x1402d8, 6436 байт),
`HookedCallObjectMethodV` (0x141c14), `GetOrResolveQueryMid` (0x143270),
глобалы `g_real_call_object_method_v`, `g_iprov_class`, `g_cache`,
`g_cache_mu`, `t_hook_depth` (TLS).

Механика:
- Через Dobby хукает `JNIEnv::CallObjectMethodV` (точка входа, через которую
  защита дёргает Java-рефлексию: читает `android.os.Process`, `Runtime`,
  system properties, проверяет окружение).
- В хуке резолвит jmethodID вызываемого метода (с кэшем и мьютексом),
  сверяет с сигнатурами «опасных» методов (`kQuerySig`), и для них
  возвращает поддельный/пустой результат вместо реального вызова.
- `t_hook_depth` (thread-local) защищает от рекурсии при собственных
  JNI-вызовах внутри хука.
- Appdome получает «чистое» окружение → детект не срабатывает → умирать
  незачем. Смерть предотвращается ДО её причины, а не после.

### 2. UnityThreadFreeze (`Hook/UnityThreadFreeze.cpp`)

Символы: `InstallUnityThreadFreeze` (0x1463d4), `hooked_pthread_create`
(0x147894), `should_park` (0x148d28), `freeze_trampoline` (0x14a39c),
`base_of` (0x14b79c), `zlvs_freeze_release_now` (0x146100),
глобалы `g_installed`, `g_released`, `g_real_pthread_create`, `kFreezeLibs`.

Механика (у нас есть упрощённая версия — расширить):
- Dobby-хук на `pthread_create`.
- `should_park(caller_lib)`: определяет библиотеку создателя потока
  (`dladdr` на return address + `base_of`) и сверяет со списком
  `kFreezeLibs` (libloader/libpglarmor — защитные либы игры).
- Потоки защиты стартуют через `freeze_trampoline` — обёртку, которая
  паркует их до события `zlvs_freeze_release_now` (или навсегда).
- `g_released` — глобальный флаг «отпустить всех», если игра дошла до
  нормальной работы.

### 3. Вспомогательное

- `LeakTrace.cpp`: их диагностика — GOT-патчи open/stat/readlink/uname/
  getaddrinfo/connect/__system_property_get с логированием стека вызовов
  (`capture_fp` через libunwind). Полезно нам как инструмент: включить,
  прогнать один раз, увидеть КАКИЕ ИМЕННО пути/проперти читает Appdome →
  дописать маскировку точечно.
- `DexFileHook`, `VMClassLoaderHook::hideXposed`, `disableHiddenApi`,
  `disableResourceLoading` — у нас аналоги есть.
- `OxideShim.java` (их Java): подменяет `libng.so` игры на свой ассет
  `libng_shim.so` при установке (CopyExecutor). У нас такого нет — если в
  новых билдах игры появится libng, повторить.

## План работ (по порядку)

### Шаг 1. Убрать брутфорс
Из `AntiDetectFix.cpp/.h` и `RuntimeHook.cpp` удалить/отключить:
- `installSyscallGuard` (seccomp)
- `installGlobalFaultSwallow` + `installCrashShield` + `installSignalGuard`
- `installInstructionTrap` (фриз exit/_exit, anon-exec)
- `installStopShield`
- `installJavaKillShield` (всё равно не работал — методы не native на API 30+)

ОСТАВИТЬ: on-disk патчи memcpy-tail (3 сайта) + kill-импорты + svc-стабы
(они не мешают и страхуют), паркинг watchdog-тредов (заменится шагом 3).

### Шаг 2. AppdomeJniTrap (основная работа)
Новый файл `Bcore/src/main/cpp/Hook/AppdomeJniTrap.cpp/.h`:

```
install(JNIEnv*):
  1. env->GetFunctions()->CallObjectMethodV → DobbyHook → HookedCallObjectMethodV
     (оригинал сохранить в g_real)
  2. TLS-флаг t_hook_depth для защиты от рекурсии

HookedCallObjectMethodV(env, obj, mid, args):
  if t_hook_depth > 0 → real(...)
  if вызов НЕ из libloader (проверка return address через dladdr) → real(...)
  resolve mid → (class, name, sig) с кэшем в g_cache под g_cache_mu
  если (name,sig) ∈ blacklist:
      Process/killProcess, Runtime/exit, Runtime/halt, System/exit,
      System/getProperty(опасные ключи), Class/forName("top.niunaijun..."),
      PackageManager-запросы про хоста
    → ALOGD("jni trap: blocked %s.%s%s from loader")
    → return nullptr (или фейковое значение по типу)
  иначе → real(...)
```

Ключевой момент: blacklist составляем по `LeakTrace`-прогону (шаг 4) +
по тому, что Appdome реально дёргает (logcat покажет blocked-строки —
добавляем итеративно).

Опасность: хукать CallObjectMethodV глобально для всего процесса нельзя —
игра и BlackBox сами активно используют JNI. Поэтому проверка
`dladdr(__builtin_return_address(0))` → `strstr(dli_fname, "libloader")`
обязательна; всё остальное пропускаем без кэш-резолва (нулевой оверхед).

Дополнительно те же хуки на: `CallStaticObjectMethodV`,
`CallStaticVoidMethodV`, `CallVoidMethodV`. На Android 12+
CallObjectMethod → CallObjectMethodV внутри libart, хук на V покрывает
обе; статик-версии хукаем отдельно.

### Шаг 3. Расширить UnityThreadFreeze
В `FileSystemHook.cpp` уже есть `hooked_pthread_create` с `calledFromLoader()`
(паркует потоки из libloader). Доработать до ZOLVS-версии:
- список либ: libloader.so + libpglarmor.so (она есть в игре — видели в
  логе `verify so libpglarmor.so`)
- trampoline-обёртка вместо мгновенного pause() (на случай, если защита
  проверяет, что поток реально стартовал, — парковать ПОСЛЕ входа в
  trampoline, как у них)

### Шаг 4. LeakTrace-прогон (1 цикл логов)
Портировать их `LeakTrace` (GOT-патч open/access/stat/readlink/fopen/
fgets/uname/getaddrinfo/connect/__system_property_get с unwind-стеком и
фильтром по caller ∈ libloader/pglarmor). Один прогон на устройстве →
полный список того, что читает Appdome → закрыть точечно через
существующий `FileSystemHook` (наши r_open/r_access уже на месте, нужно
только расширить фильтрацию содержимого и фейковые значения пропертей).

### Шаг 5. Прогон и итерации
- Собрать, поставить с полным сносом данных приложения (чтобы игра
  переустановилась в коробке чистой).
- Лог: строки `jni trap: blocked ...` покажут, что именно пыталась
  сделать защита → расширяем blacklist до тишины + отсутствия
  QOITXKOQ/App Died.
- Критерий победы: libmain/libunity грузятся, игра рисует первый кадр,
  нет `App Died` в течение 60 секунд.

## Файлы, которые меняем

- `Bcore/src/main/cpp/Hook/AntiDetectFix.cpp/.h` — выпилить брутфорс (шаг 1)
- `Bcore/src/main/cpp/Hook/AppdomeJniTrap.cpp/.h` — НОВЫЙ (шаг 2)
- `Bcore/src/main/cpp/Hook/RuntimeHook.cpp` — вызов install-трапа при
  nativeLoad("libloader") вместо текущего зоопарка
- `Bcore/src/main/cpp/Hook/FileSystemHook.cpp` — расширение thread-freeze
  (шаг 3) + фильтры по результатам LeakTrace (шаг 4)
- `Bcore/src/main/cpp/Hook/LeakTrace.cpp/.h` — НОВЫЙ, временный (шаг 4),
  под флагом сборки, чтобы не тащить в релиз
- CMakeLists уже собирает всё из Hook/ через aux_source_directory — новые
  файлы подхватятся сами.

## Риски

- Проверка caller через `__builtin_return_address` при -O2 может дать не
  тот кадр, если компилятор заинлайнил обёртку → держать HookedCall* с
  `__attribute__((noinline))` и при необходимости идти на кадр выше через
  x29-цепочку (как в их capture_fp).
- Если защита читает окружение чистым native (open/read напрямую, без JNI)
  — это закрывает шаг 4 (LeakTrace → фильтры FileSystemHook).

## Что НЕ трогаем

- On-disk патчи libloader (memcpy/kill-импорты/svc) — оставляем как есть,
  они не конфликтуют с JNI-трапом.
- BoxApp/Installer/MainActivity — не связаны с проблемой.
- seccomp/signal/exit-брутфорс — удаляем, он дал только чёрные экраны.
