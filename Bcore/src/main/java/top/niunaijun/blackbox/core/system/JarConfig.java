package top.niunaijun.blackbox.core.system;

import java.io.File;

public class JarConfig {
    public static final long CACHE_CLEANUP_INTERVAL_MS = 300000;
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static final boolean ENABLE_ASYNC_LOADING = true;
    public static final boolean ENABLE_FILE_HASHING = true;
    public static final boolean ENABLE_SIZE_VALIDATION = true;
    public static final int MAX_BUFFER_SIZE = 32768;
    public static final int MAX_CACHE_SIZE = 10;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long MAX_RETRY_DELAY_MS = 5000;
    public static final int MIN_BUFFER_SIZE = 1024;
    public static final long RETRY_DELAY_MS = 1000;
    public static final JarDefinition EMPTY_JAR = new JarDefinition("empty.jar", "empty.apk", 100, "Empty JAR for fallback DEX loading", true);
    public static final JarDefinition JUNIT_JAR = new JarDefinition("junit.jar", "junit.apk", 1000, "JUnit testing framework support", false);

    public static class JarDefinition {
        private final String assetName;
        private final String description;
        private final String fileName;
        private final long minSize;
        private final boolean required;

        public JarDefinition(String str, String str2, long j, String str3, boolean z6) {
            this.assetName = str;
            this.fileName = str2;
            this.minSize = j;
            this.description = str3;
            this.required = z6;
        }

        public String getAssetName() {
            return this.assetName;
        }

        public String getDescription() {
            return this.description;
        }

        public String getFileName() {
            return this.fileName;
        }

        public long getMinSize() {
            return this.minSize;
        }

        public boolean isRequired() {
            return this.required;
        }
    }

    public static JarDefinition getJarDefinition(String str) {
        for (JarDefinition jarDefinition : getRequiredJars()) {
            if (jarDefinition.getAssetName().equals(str)) {
                return jarDefinition;
            }
        }
        return null;
    }

    public static int getOptimalBufferSize() {
        long jMaxMemory = Runtime.getRuntime().maxMemory();
        if (jMaxMemory > 536870912) {
            return MAX_BUFFER_SIZE;
        }
        if (jMaxMemory > 268435456) {
            return DEFAULT_BUFFER_SIZE;
        }
        return 1024;
    }

    public static JarDefinition[] getRequiredJars() {
        return new JarDefinition[]{EMPTY_JAR, JUNIT_JAR};
    }

    public static boolean validateFile(File file, JarDefinition jarDefinition) {
        return file != null && file.exists() && file.length() >= jarDefinition.getMinSize();
    }
}
