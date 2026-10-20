package top.niunaijun.blackbox.utils;

import top.niunaijun.blackbox.utils.Slog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import top.niunaijun.blackbox.BlackBoxCore;

public class AttributionSourceUtils {
    private static final String TAG = "AttributionSourceUtils";

    public static Object createSafeAttributionSource() {
        try {
            Class<?> cls = Class.forName("android.content.AttributionSource");
            try {
                try {
                    Constructor<?> declaredConstructor = cls.getDeclaredConstructor(Integer.TYPE, String.class);
                    declaredConstructor.setAccessible(true);
                    return declaredConstructor.newInstance(Integer.valueOf(BlackBoxCore.getHostUid()), BlackBoxCore.getHostPkg());
                } catch (Exception unused) {
                    Constructor<?> declaredConstructor2 = cls.getDeclaredConstructor(null);
                    declaredConstructor2.setAccessible(true);
                    Object objNewInstance = declaredConstructor2.newInstance(null);
                    fixAttributionSourceUid(objNewInstance);
                    return objNewInstance;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Could not create safe AttributionSource: " + e.getMessage());
                return null;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error creating safe AttributionSource: " + e.getMessage());
            return null;
        }
    }

    public static void fixAttributionSourceInArgs(Object[] args) {
        if (args == null) {
            return;
        }
        for (Object who : args) {
            if (who != null && who.getClass().getName().contains("AttributionSource")) {
                try {
                    fixAttributionSourceUid(who);
                    Slog.d(TAG, "Fixed AttributionSource UID in method arguments");
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to fix AttributionSource in args: " + e.getMessage());
                }
            }
        }
        for (Object proxyInvocation : args) {
            if (proxyInvocation != null && proxyInvocation.getClass().getName().contains("Bundle")) {
                try {
                    fixAttributionSourceInBundle(proxyInvocation);
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to fix AttributionSource in Bundle: " + e.getMessage());
                }
            }
        }
    }

    public static void fixAttributionSourceInBundle(Object who) {
        if (who == null) {
            return;
        }
        try {
            for (String str : (Set) who.getClass().getMethod("keySet", null).invoke(who, null)) {
                try {
                    Object objInvoke = who.getClass().getMethod("get", String.class).invoke(who, str);
                    if (objInvoke != null && objInvoke.getClass().getName().contains("AttributionSource")) {
                        fixAttributionSourceUid(objInvoke);
                        Slog.d(TAG, "Fixed AttributionSource UID in Bundle key: " + str);
                    }
                } catch (Exception unused) {
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error fixing AttributionSource in Bundle: " + e.getMessage());
        }
    }

    public static void fixAttributionSourceUid(Object who) {
        if (who == null) {
            return;
        }
        try {
            Class<?> cls = who.getClass();
            String[] strArr = {"mUid", "uid", "mCallingUid", "callingUid", "mSourceUid", "sourceUid"};
            for (int i = 0; i < 6; i++) {
                String str = strArr[i];
                try {
                    Field declaredField = cls.getDeclaredField(str);
                    declaredField.setAccessible(true);
                    declaredField.set(who, Integer.valueOf(BlackBoxCore.getHostUid()));
                    Slog.d(TAG, "Fixed AttributionSource UID via field: " + str);
                    break;
                } catch (NoSuchFieldException unused) {
                }
            }
            try {
                Method declaredMethod = cls.getDeclaredMethod("setUid", Integer.TYPE);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(who, Integer.valueOf(BlackBoxCore.getHostUid()));
                Slog.d(TAG, "Fixed AttributionSource UID via setter method");
            } catch (Exception unused2) {
            }
            String[] strArr2 = {"mPackageName", "packageName", "mSourcePackage", "sourcePackage"};
            for (int i = 0; i < 4; i++) {
                String str2 = strArr2[i];
                try {
                    Field declaredField2 = cls.getDeclaredField(str2);
                    declaredField2.setAccessible(true);
                    declaredField2.set(who, BlackBoxCore.getHostPkg());
                    Slog.d(TAG, "Fixed AttributionSource package name via field: " + str2);
                    return;
                } catch (NoSuchFieldException unused3) {
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error fixing AttributionSource UID: " + e.getMessage());
        }
    }

    public static boolean validateAttributionSource(Object who) {
        int iIntValue;
        if (who == null) {
            return false;
        }
        try {
            Class<?> cls = who.getClass();
            String[] strArr = {"mUid", "uid", "mCallingUid", "callingUid", "mSourceUid", "sourceUid"};
            for (int i = 0; i < 6; i++) {
                try {
                    Field declaredField = cls.getDeclaredField(strArr[i]);
                    declaredField.setAccessible(true);
                    Object proxyInvocation = declaredField.get(who);
                    if ((proxyInvocation instanceof Integer) && (iIntValue = ((Integer) proxyInvocation).intValue()) > 0) {
                        Slog.d(TAG, "AttributionSource UID validation passed: " + iIntValue);
                        return true;
                    }
                } catch (Exception unused) {
                }
            }
            Slog.w(TAG, "AttributionSource validation failed, attempting to fix");
            fixAttributionSourceUid(who);
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Error validating AttributionSource: " + e.getMessage());
            return false;
        }
    }
}
