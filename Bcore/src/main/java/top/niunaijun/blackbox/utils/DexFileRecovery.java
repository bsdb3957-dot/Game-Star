package top.niunaijun.blackbox.utils;

import top.niunaijun.blackbox.utils.Slog;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.system.JarConfig;

public class DexFileRecovery {
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final String TAG = "DexFileRecovery";
    private static final Map<String, RecoveryResult> sRecoveryCache = new HashMap();
    private static final List<RecoveryStrategy> sRecoveryStrategies = new ArrayList();

    public static class AlternativeApkStrategy implements RecoveryStrategy {
        public AlternativeApkStrategy(int i) {
            this();
        }

        @Override
        public RecoveryResult attemptRecovery(String str) {
            File[] fileArrListFiles;
            try {
                File parentFile = new File(str).getParentFile();
                if (parentFile != null && parentFile.exists() && (fileArrListFiles = parentFile.listFiles()) != null) {
                    for (File file : fileArrListFiles) {
                        if (file.getName().endsWith(".apk") && file.length() > 1000000 && !file.getAbsolutePath().equals(str) && DexFileRecovery.isValidApkFile(file)) {
                            Slog.d(DexFileRecovery.TAG, "Found valid alternative APK: " + file.getAbsolutePath());
                            return new RecoveryResult(file.getAbsolutePath(), getName());
                        }
                    }
                }
                return new RecoveryResult("No valid alternative APK files found");
            } catch (Exception e) {
                return new RecoveryResult("Error in alternative APK strategy: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Alternative APK";
        }

        @Override
        public int getPriority() {
            return 100;
        }

        private AlternativeApkStrategy() {
        }
    }

    public static class BackupRestoreStrategy implements RecoveryStrategy {
        public BackupRestoreStrategy(int i) {
            this();
        }

        private String findBackupFile(String str) {
            File[] fileArrListFiles;
            try {
                File file = new File(str);
                File parentFile = file.getParentFile();
                final String strReplaceFirst = file.getName().replaceFirst("[.][^.]+$", "");
                if (parentFile == null || !parentFile.exists() || (fileArrListFiles = parentFile.listFiles(new FilenameFilter() { // from class: top.niunaijun.blackbox.utils.b
                    @Override
                    public final boolean accept(File file2, String str2) {
                        return DexFileRecovery.BackupRestoreStrategy.lambda$findBackupFile$0(strReplaceFirst, file2, str2);
                    }
                })) == null || fileArrListFiles.length <= 0) {
                    return null;
                }
                File file2 = fileArrListFiles[0];
                for (File file3 : fileArrListFiles) {
                    if (file3.lastModified() > file2.lastModified()) {
                        file2 = file3;
                    }
                }
                return file2.getAbsolutePath();
            } catch (Exception e) {
                Slog.w(DexFileRecovery.TAG, "Error finding backup file: " + e.getMessage());
                return null;
            }
        }

        public static boolean lambda$findBackupFile$0(String str, File file, String str2) {
            if (str2.startsWith(str)) {
                return str2.endsWith(".bak") || str2.endsWith(".backup") || str2.endsWith(".old");
            }
            return false;
        }

        @Override
        public RecoveryResult attemptRecovery(String str) {
            try {
                String strFindBackupFile = findBackupFile(str);
                if (strFindBackupFile == null || !DexFileRecovery.isValidApkFile(new File(strFindBackupFile))) {
                    return new RecoveryResult("No valid backup files found");
                }
                Slog.d(DexFileRecovery.TAG, "Found valid backup file: ".concat(strFindBackupFile));
                return new RecoveryResult(strFindBackupFile, getName());
            } catch (Exception e) {
                return new RecoveryResult("Error in backup restore strategy: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Backup Restore";
        }

        @Override
        public int getPriority() {
            return 20;
        }

        private BackupRestoreStrategy() {
        }
    }

    public static class DexExtractionStrategy implements RecoveryStrategy {
        public DexExtractionStrategy(int i) {
            this();
        }

        /* JADX WARN: Code duplicated, block: B:59:0x0097 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        private String extractDexFromApk(File file) {
            try {
                ZipFile zipFile = new ZipFile(file);
                try {
                    ZipEntry entry = zipFile.getEntry("classes.dex");
                    if (entry == null) {
                        zipFile.close();
                        return null;
                    }
                    File file2 = new File(BlackBoxCore.getContext().getCacheDir(), "dex_recovery");
                    if (!file2.exists()) {
                        file2.mkdirs();
                    }
                    File file3 = new File(file2, "classes_" + System.currentTimeMillis() + ".dex");
                    InputStream inputStream = zipFile.getInputStream(entry);
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file3);
                        try {
                            byte[] bArr = new byte[JarConfig.DEFAULT_BUFFER_SIZE];
                            while (true) {
                                int i = inputStream.read(bArr);
                                if (i == -1) {
                                    fileOutputStream.close();
                                    inputStream.close();
                                    Slog.d(DexFileRecovery.TAG, "Extracted DEX to: " + file3.getAbsolutePath());
                                    String absolutePath = file3.getAbsolutePath();
                                    zipFile.close();
                                    return absolutePath;
                                }
                                fileOutputStream.write(bArr, 0, i);
                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (Throwable th) {
                                        th.addSuppressed(th);
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th) {
                            try {
                                fileOutputStream.close();
                            } catch (Throwable th) {
                                th.addSuppressed(th);
                            }
                            throw th;
                        }
                    } catch (Throwable th) {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        throw th;
                    }
                } catch (Throwable th) {
                    try {
                        zipFile.close();
                    } catch (Throwable th) {
                        th.addSuppressed(th);
                    }
                    throw th;
                }
            } catch (Exception e) {
                Slog.w(DexFileRecovery.TAG, "Failed to extract DEX from APK: " + e.getMessage());
                return null;
            }
            Slog.w(DexFileRecovery.TAG, "Failed to extract DEX from APK: " + e.getMessage());
            return null;
        }

        @Override
        public RecoveryResult attemptRecovery(String str) {
            try {
                File file = new File(str);
                if (file.exists() && file.getName().endsWith(".apk")) {
                    String strExtractDexFromApk = extractDexFromApk(file);
                    if (strExtractDexFromApk == null) {
                        return new RecoveryResult("Failed to extract DEX from corrupted APK");
                    }
                    Slog.d(DexFileRecovery.TAG, "Successfully extracted DEX from corrupted APK: ".concat(strExtractDexFromApk));
                    return new RecoveryResult(strExtractDexFromApk, getName());
                }
                return new RecoveryResult("Not an APK file or file doesn't exist");
            } catch (Exception e) {
                return new RecoveryResult("Error in DEX extraction strategy: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "DEX Extraction";
        }

        @Override
        public int getPriority() {
            return 40;
        }

        private DexExtractionStrategy() {
        }
    }

    public static class HostAppApkStrategy implements RecoveryStrategy {
        public HostAppApkStrategy(int i) {
            this();
        }

        @Override
        public RecoveryResult attemptRecovery(String str) {
            ApplicationInfo applicationInfo;
            try {
                Context context = BlackBoxCore.getContext();
                if (context != null && (applicationInfo = context.getApplicationInfo()) != null && applicationInfo.sourceDir != null) {
                    File file = new File(applicationInfo.sourceDir);
                    if (file.exists() && DexFileRecovery.isValidApkFile(file)) {
                        Slog.d(DexFileRecovery.TAG, "Using host app APK as fallback: " + applicationInfo.sourceDir);
                        return new RecoveryResult(applicationInfo.sourceDir, getName());
                    }
                }
                return new RecoveryResult("Host app APK not available or invalid");
            } catch (Exception e) {
                return new RecoveryResult("Error in host app APK strategy: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Host App APK";
        }

        @Override
        public int getPriority() {
            return 80;
        }

        private HostAppApkStrategy() {
        }
    }

    public interface RecoveryStrategy {
        RecoveryResult attemptRecovery(String str);

        String getName();

        int getPriority();
    }

    public static class SystemApkStrategy implements RecoveryStrategy {
        public SystemApkStrategy(int i) {
            this();
        }

        public static boolean lambda$attemptRecovery$0(File file, String str) {
            return str.endsWith(".apk");
        }

        @Override
        public RecoveryResult attemptRecovery(String str) {
            File[] fileArrListFiles;
            File[] fileArrListFiles2;
            try {
                String[] strArr = {"/system/app", "/system/priv-app", "/vendor/app", "/product/app"};
                for (int i = 0; i < 4; i++) {
                    File file = new File(strArr[i]);
                    if (file.exists() && file.isDirectory() && (fileArrListFiles = file.listFiles()) != null) {
                        for (File file2 : fileArrListFiles) {
                            if (file2.isDirectory() && (fileArrListFiles2 = file2.listFiles(new c(0))) != null && fileArrListFiles2.length > 0) {
                                File file3 = fileArrListFiles2[0];
                                if (DexFileRecovery.isValidApkFile(file3)) {
                                    Slog.d(DexFileRecovery.TAG, "Found valid system APK: " + file3.getAbsolutePath());
                                    return new RecoveryResult(file3.getAbsolutePath(), getName());
                                }
                            }
                        }
                    }
                }
                return new RecoveryResult("No valid system APK files found");
            } catch (Exception e) {
                return new RecoveryResult("Error in system APK strategy: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "System APK";
        }

        @Override
        public int getPriority() {
            return 60;
        }

        private SystemApkStrategy() {
        }
    }

    static {
        initializeRecoveryStrategies();
    }

    public static void clearCache() {
        sRecoveryCache.clear();
        Slog.d(TAG, "Recovery cache cleared");
    }

    public static RecoveryResult forceRecovery(String str) {
        sRecoveryCache.remove(str);
        return recoverDexFile(str);
    }

    public static String getRecoveryStats() {
        Iterator<RecoveryResult> it = sRecoveryCache.values().iterator();
        int i = 0;
        int i = 0;
        while (it.hasNext()) {
            if (it.next().success) {
                i++;
            } else {
                i++;
            }
        }
        return "Recovery Stats - Successful: " + i + ", Failed: " + i + ", Total Attempts: " + sRecoveryCache.size();
    }

    private static void initializeRecoveryStrategies() {
        List<RecoveryStrategy> list = sRecoveryStrategies;
        int i = 0;
        list.add(new AlternativeApkStrategy(i));
        list.add(new HostAppApkStrategy(i));
        list.add(new SystemApkStrategy(i));
        list.add(new DexExtractionStrategy(i));
        list.add(new BackupRestoreStrategy(i));
        list.sort(new a());
        Slog.d(TAG, "Initialized " + list.size() + " recovery strategies");
    }

    public static boolean isValidApkFile(File file) {
        if (file != null && file.exists()) {
            try {
                if (file.length() < 1000000) {
                    return false;
                }
                try {
                    ZipFile zipFile = new ZipFile(file);
                    try {
                        ZipEntry entry = zipFile.getEntry("classes.dex");
                        boolean z6 = entry != null && entry.getSize() > 0;
                        zipFile.close();
                        return z6;
                    } catch (Throwable th) {
                        try {
                            zipFile.close();
                        } catch (Throwable th) {
                            th.addSuppressed(th);
                        }
                        throw th;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "APK validation failed for " + file.getAbsolutePath() + ": " + e.getMessage());
                    return false;
                }
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder("Error validating APK file ");
                sb.append(file.getAbsolutePath());
                sb.append(": ");
                Slog.w(TAG, sb + e.getMessage());
            }
        }
        return false;
    }

    public static int lambda$initializeRecoveryStrategies$0(RecoveryStrategy recoveryStrategy, RecoveryStrategy recoveryStrategy2) {
        return Integer.compare(recoveryStrategy2.getPriority(), recoveryStrategy.getPriority());
    }

    public static RecoveryResult recoverDexFile(String str) {
        if (str == null) {
            return new RecoveryResult("Corrupted file path is null");
        }
        Map<String, RecoveryResult> map = sRecoveryCache;
        if (map.containsKey(str)) {
            RecoveryResult recoveryResult = map.get(str);
            Slog.d(TAG, "Using cached recovery result for " + str + ": " + recoveryResult);
            return recoveryResult;
        }
        Slog.w(TAG, "Attempting to recover corrupted DEX file: ".concat(str));
        for (RecoveryStrategy recoveryStrategy : sRecoveryStrategies) {
            try {
                Slog.d(TAG, "Trying recovery strategy: " + recoveryStrategy.getName());
                RecoveryResult recoveryResultAttemptRecovery = recoveryStrategy.attemptRecovery(str);
                if (recoveryResultAttemptRecovery.success) {
                    Slog.d(TAG, "Recovery successful via " + recoveryStrategy.getName() + ": " + recoveryResultAttemptRecovery.recoveredFilePath);
                    sRecoveryCache.put(str, recoveryResultAttemptRecovery);
                    return recoveryResultAttemptRecovery;
                }
                Slog.w(TAG, "Recovery failed via " + recoveryStrategy.getName() + ": " + recoveryResultAttemptRecovery.errorMessage);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder("Recovery strategy ");
                sb.append(recoveryStrategy.getName());
                sb.append(" threw exception: ");
                Slog.w(TAG, sb + e.getMessage());
            }
        }
        RecoveryResult recoveryResult2 = new RecoveryResult("All recovery strategies failed for ".concat(str));
        sRecoveryCache.put(str, recoveryResult2);
        return recoveryResult2;
    }

    public static class RecoveryResult {
        public final String errorMessage;
        public final String recoveredFilePath;
        public final String recoveryMethod;
        public final boolean success;

        public RecoveryResult(String str, String str2) {
            this.recoveredFilePath = str;
            this.recoveryMethod = str2;
            this.success = str != null;
            this.errorMessage = null;
        }

        public String toString() {
            if (!this.success) {
                return "Recovery failed: " + this.errorMessage;
            }
            return "Recovery successful via " + this.recoveryMethod + ": " + this.recoveredFilePath;
        }

        public RecoveryResult(String str) {
            this.recoveredFilePath = null;
            this.recoveryMethod = null;
            this.success = false;
            this.errorMessage = str;
        }
    }
}
