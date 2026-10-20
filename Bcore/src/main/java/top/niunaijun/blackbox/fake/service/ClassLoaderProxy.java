package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.fragment.app.z0;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.DexFileRecovery;
import top.niunaijun.blackbox.utils.Slog;

public class ClassLoaderProxy extends ClassInvocationStub {
    private static final String TAG = "ClassLoaderProxy";
    private static final Map<String, Class<?>> sClassCache = new HashMap();
    private static final Map<String, Boolean> sDexFileCache = new HashMap();
    private static final List<ClassLoader> sFallbackClassLoaders = new ArrayList();

    @ProxyMethod("findClass")
    public static class FindClass extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            String str = (String) args[0];
            Map map = ClassLoaderProxy.sClassCache;
            if (map.containsKey(str)) {
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: findClass returning cached class: " + str);
                return map.get(str);
            }
            if (ClassLoaderProxy.isProblematicClass(str)) {
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: findClass called for problematic class: " + str + ", returning null");
                return null;
            }
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    map.put(str, (Class) objInvoke);
                    Slog.d(ClassLoaderProxy.TAG, "ClassLoader: findClass successful for: " + str);
                    return objInvoke;
                }
            } catch (Exception e) {
                new StringBuilder("ClassLoader: findClass failed for ").append(str).append(", trying fallbacks: ").append(e.getMessage());
                Slog.w(ClassLoaderProxy.TAG, new StringBuilder("ClassLoader: findClass failed for ").append(str).append(", trying fallbacks: ").toString())
            }
            Class clsTryFallbackClassLoaders = ClassLoaderProxy.tryFallbackClassLoaders(str);
            if (clsTryFallbackClassLoaders != null) {
                ClassLoaderProxy.sClassCache.put(str, clsTryFallbackClassLoaders);
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: findClass successful via fallback for: " + str);
                return clsTryFallbackClassLoaders;
            }
            Slog.w(ClassLoaderProxy.TAG, "ClassLoader: findClass failed for " + str + ", returning null");
            return null;
        }
    }

    @ProxyMethod("forName")
    public static class ForName extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            String str = (String) args[0];
            Map map = ClassLoaderProxy.sClassCache;
            if (map.containsKey(str)) {
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: forName returning cached class: " + str);
                return map.get(str);
            }
            if (ClassLoaderProxy.isProblematicClass(str)) {
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: forName called for problematic class: " + str + ", returning null");
                return null;
            }
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    map.put(str, (Class) objInvoke);
                    Slog.d(ClassLoaderProxy.TAG, "ClassLoader: forName successful for: " + str);
                    return objInvoke;
                }
            } catch (Exception e) {
                new StringBuilder("ClassLoader: forName failed for ").append(str).append(", trying fallbacks: ").append(e.getMessage());
                Slog.w(ClassLoaderProxy.TAG, new StringBuilder("ClassLoader: forName failed for ").append(str).append(", trying fallbacks: ").toString())
            }
            Class clsTryFallbackClassLoaders = ClassLoaderProxy.tryFallbackClassLoaders(str);
            if (clsTryFallbackClassLoaders != null) {
                ClassLoaderProxy.sClassCache.put(str, clsTryFallbackClassLoaders);
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: forName successful via fallback for: " + str);
                return clsTryFallbackClassLoaders;
            }
            Slog.w(ClassLoaderProxy.TAG, "ClassLoader: forName failed for " + str + ", returning null");
            return null;
        }
    }

    @ProxyMethod("loadClass")
    public static class LoadClass extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            String str = (String) args[0];
            if (args.length > 1) {
                ((Boolean) args[1]).getClass();
            }
            Map map = ClassLoaderProxy.sClassCache;
            if (map.containsKey(str)) {
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: loadClass returning cached class: " + str);
                return map.get(str);
            }
            if (ClassLoaderProxy.isProblematicClass(str)) {
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: loadClass called for problematic class: " + str + ", returning null");
                return null;
            }
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    map.put(str, (Class) objInvoke);
                    Slog.d(ClassLoaderProxy.TAG, "ClassLoader: loadClass successful for: " + str);
                    return objInvoke;
                }
            } catch (Exception e) {
                new StringBuilder("ClassLoader: loadClass failed for ").append(str).append(", trying fallbacks: ").append(e.getMessage());
                Slog.w(ClassLoaderProxy.TAG, new StringBuilder("ClassLoader: loadClass failed for ").append(str).append(", trying fallbacks: ").toString())
            }
            Class clsTryFallbackClassLoaders = ClassLoaderProxy.tryFallbackClassLoaders(str);
            if (clsTryFallbackClassLoaders != null) {
                ClassLoaderProxy.sClassCache.put(str, clsTryFallbackClassLoaders);
                Slog.d(ClassLoaderProxy.TAG, "ClassLoader: loadClass successful via fallback for: " + str);
                return clsTryFallbackClassLoaders;
            }
            Slog.w(ClassLoaderProxy.TAG, "ClassLoader: loadClass failed for " + str + ", returning null");
            return null;
        }
    }

    @ProxyMethod("loadDexFile")
    public static class LoadDexFile extends MethodHook {
        private String extractCorruptedFilePathFromError(Exception exc, Object who) {
            int iIndexOf;
            try {
                String message = exc.getMessage();
                if (message != null && message.contains("/data/app/") && (iIndexOf = message.indexOf("/data/app/")) >= 0) {
                    int iIndexOf2 = message.indexOf(" ", iIndexOf);
                    if (iIndexOf2 < 0) {
                        iIndexOf2 = message.length();
                    }
                    return message.substring(iIndexOf, iIndexOf2);
                }
                try {
                    Field declaredField = who.getClass().getDeclaredField("pathList");
                    declaredField.setAccessible(true);
                    Object proxyInvocation = declaredField.get(who);
                    if (proxyInvocation == null) {
                        return null;
                    }
                    Field declaredField2 = proxyInvocation.getClass().getDeclaredField("dexElements");
                    declaredField2.setAccessible(true);
                    Object[] args = (Object[]) declaredField2.get(proxyInvocation);
                    if (args == null || args.length <= 0) {
                        return null;
                    }
                    Field declaredField3 = args[0].getClass().getDeclaredField("path");
                    declaredField3.setAccessible(true);
                    return (String) declaredField3.get(args[0]);
                } catch (Exception e) {
                    Slog.w(ClassLoaderProxy.TAG, "Could not extract path via reflection: " + e.getMessage());
                    return null;
                }
            } catch (Exception e) {
                z0.o("Error extracting corrupted file path: ", e, ClassLoaderProxy.TAG);
                return null;
            }
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("classes.dex") && message.contains("Entry not found")) {
                    Slog.w(ClassLoaderProxy.TAG, "DexPathList: classes.dex entry not found, attempting recovery");
                    String strExtractCorruptedFilePathFromError = extractCorruptedFilePathFromError(e, who);
                    if (strExtractCorruptedFilePathFromError != null) {
                        DexFileRecovery.RecoveryResult recoveryResultRecoverDexFile = DexFileRecovery.recoverDexFile(strExtractCorruptedFilePathFromError);
                        if (recoveryResultRecoverDexFile.success) {
                            Slog.d(ClassLoaderProxy.TAG, "DexPathList: Successfully recovered DEX file via " + recoveryResultRecoverDexFile.recoveryMethod);
                            Object objCreateFallbackDexPathList = ClassLoaderProxy.createFallbackDexPathList(who, recoveryResultRecoverDexFile.recoveredFilePath);
                            if (objCreateFallbackDexPathList != null) {
                                return objCreateFallbackDexPathList;
                            }
                        } else {
                            Slog.w(ClassLoaderProxy.TAG, "DexPathList: DEX file recovery failed: " + recoveryResultRecoverDexFile.errorMessage);
                        }
                    }
                }
                z0.o("DexPathList: loadDexFile failed, returning null: ", e, ClassLoaderProxy.TAG);
                return null;
            }
        }
    }

    @ProxyMethod("openDexFile")
    public static class OpenDexFile extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            String str = (String) args[0];
            if (!ClassLoaderProxy.isValidDexFile(str)) {
                Slog.w(ClassLoaderProxy.TAG, "DexFile: Invalid DEX file detected: " + str + ", attempting recovery");
                String strRecoverDexFile = ClassLoaderProxy.recoverDexFile(str);
                if (strRecoverDexFile == null) {
                    Slog.e(ClassLoaderProxy.TAG, "DexFile: Could not recover DEX file: " + str);
                    return null;
                }
                args[0] = strRecoverDexFile;
                Slog.d(ClassLoaderProxy.TAG, "DexFile: Using recovered DEX file: ".concat(strRecoverDexFile));
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                new StringBuilder("DexFile: openDexFile failed for ").append(str).append(", returning null: ").append(e.getMessage());
                Slog.w(ClassLoaderProxy.TAG, new StringBuilder("DexFile: openDexFile failed for ").append(str).append(", returning null: ").toString())
                return null;
            }
        }
    }

    public ClassLoaderProxy() {
        initializeFallbackClassLoaders();
    }

    public static void clearClassCache() {
        sClassCache.clear();
        sDexFileCache.clear();
        Slog.d(TAG, "Class and DEX file caches cleared");
    }

    public static Object createFallbackDexPathList(Object who, String str) {
        try {
            Slog.d(TAG, "Creating fallback DexPathList with: " + str);
            return null;
        } catch (Exception e) {
            z0.o("Error creating fallback DexPathList: ", e, TAG);
            return null;
        }
    }

    private static String findAlternativeApkFile(String str) {
        File[] fileArrListFiles;
        try {
            File parentFile = new File(str).getParentFile();
            if (parentFile == null || !parentFile.exists() || (fileArrListFiles = parentFile.listFiles()) == null) {
                return null;
            }
            for (File file : fileArrListFiles) {
                if (file.getName().endsWith(".apk") && file.length() > 1000000) {
                    Slog.d(TAG, "Found alternative APK file: " + file.getAbsolutePath());
                    return file.getAbsolutePath();
                }
            }
            return null;
        } catch (Exception e) {
            z0.o("Error finding alternative APK file: ", e, TAG);
            return null;
        }
    }

    public static String getCacheStats() {
        return "Class Cache: " + sClassCache.size() + " entries, DEX Cache: " + sDexFileCache.size() + " entries";
    }

    private void initializeFallbackClassLoaders() {
        ClassLoader classLoader;
        try {
            Context context = BlackBoxCore.getContext();
            if (context != null && (classLoader = context.getClassLoader()) != null) {
                sFallbackClassLoaders.add(classLoader);
                Slog.d(TAG, "Added host class loader as fallback");
            }
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (systemClassLoader != null) {
                sFallbackClassLoaders.add(systemClassLoader);
                Slog.d(TAG, "Added system class loader as fallback");
            }
            ClassLoader classLoader2 = Object.class.getClassLoader();
            if (classLoader2 != null) {
                sFallbackClassLoaders.add(classLoader2);
                Slog.d(TAG, "Added boot class loader as fallback");
            }
        } catch (Exception e) {
            z0.o("Failed to initialize fallback class loaders: ", e, TAG);
        }
    }

    public static boolean isProblematicClass(String str) {
        if (str == null) {
            return false;
        }
        return str.contains("kotlinx.coroutines.test") || str.contains("com.mediatek.perfservice") || str.contains("com.mediatek.powerhalmgr") || str.contains("androidx.datastore.preferences.protobuf") || str.contains("NewInstanceSchemaFull") || str.contains("ExtensionSchemaFull") || str.contains("org.robolectric") || str.contains("Robolectric") || str.contains("TestMainDispatcherFactory") || str.contains("PerfServiceWrapper") || str.contains("PowerHalMgrFactory");
    }

    public static boolean isValidDexFile(String str) {
        if (str == null) {
            return false;
        }
        Map<String, Boolean> map = sDexFileCache;
        if (map.containsKey(str)) {
            return map.get(str).booleanValue();
        }
        try {
            File file = new File(str);
            if (!file.exists()) {
                map.put(str, Boolean.FALSE);
                return false;
            }
            if (!str.endsWith(".apk") && !str.endsWith(".zip")) {
                map.put(str, Boolean.TRUE);
                return true;
            }
            boolean zValidateApkFile = validateApkFile(file);
            map.put(str, Boolean.valueOf(zValidateApkFile));
            return zValidateApkFile;
        } catch (Exception e) {
            new StringBuilder("Error validating DEX file ").append(str).append(": ").append(e.getMessage());
            Slog.w(TAG, new StringBuilder("Error validating DEX file ").append(str).append(": ").toString())
            sDexFileCache.put(str, Boolean.FALSE);
            return false;
        }
    }

    public static String recoverDexFile(String str) {
        ApplicationInfo applicationInfo;
        try {
            String strFindAlternativeApkFile = findAlternativeApkFile(str);
            if (strFindAlternativeApkFile != null) {
                return strFindAlternativeApkFile;
            }
            Context context = BlackBoxCore.getContext();
            if (context == null || (applicationInfo = context.getApplicationInfo()) == null || applicationInfo.sourceDir == null) {
                return null;
            }
            Slog.d(TAG, "Using host app APK as DEX fallback: " + applicationInfo.sourceDir);
            return applicationInfo.sourceDir;
        } catch (Exception e) {
            z0.o("Error recovering DEX file: ", e, TAG);
            return null;
        }
    }

    private static Object recoverDexPathList(Object who, Object[] args) {
        ApplicationInfo applicationInfo;
        try {
            Context context = BlackBoxCore.getContext();
            if (context == null || (applicationInfo = context.getApplicationInfo()) == null || applicationInfo.sourceDir == null) {
                return null;
            }
            Slog.d(TAG, "Recovering DexPathList using host APK: " + applicationInfo.sourceDir);
            return createFallbackDexPathList(who, applicationInfo.sourceDir);
        } catch (Exception e) {
            z0.o("Error recovering DexPathList: ", e, TAG);
            return null;
        }
    }

    public static Class<?> tryFallbackClassLoaders(String str) {
        for (ClassLoader classLoader : sFallbackClassLoaders) {
            try {
                Class<?> clsLoadClass = classLoader.loadClass(str);
                if (clsLoadClass != null) {
                    Slog.d(TAG, "Successfully loaded class " + str + " via fallback loader: " + classLoader.getClass().getSimpleName());
                    return clsLoadClass;
                }
                continue;
            } catch (Exception unused) {
            }
        }
        return null;
    }

    private static boolean validateApkFile(File file) {
        try {
            return file.length() > 1000;
        } catch (Exception unused) {
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        Slog.d(TAG, "ClassLoader proxy injected for DEX file corruption prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
