package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ISettingsProviderProxy extends ClassInvocationStub {
    public static final String TAG = "ISettingsProviderProxy";

    @ProxyMethod("getFloat")
    public static class GetFloat extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getFloat, returning safe default: ".concat(message));
                return Float.valueOf(1.0f);
            }
        }
    }

    @ProxyMethod("getFloatForUser")
    public static class GetFloatForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getFloatForUser, returning safe default: ".concat(message));
                return Float.valueOf(1.0f);
            }
        }
    }

    @ProxyMethod("getInt")
    public static class GetInt extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getInt, returning safe default: ".concat(message));
                return 1;
            }
        }
    }

    @ProxyMethod("getIntForUser")
    public static class GetIntForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getIntForUser, returning safe default: ".concat(message));
                return 1;
            }
        }
    }

    @ProxyMethod("getLong")
    public static class GetLong extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getLong, returning safe default: ".concat(message));
                return 1L;
            }
        }
    }

    @ProxyMethod("getLongForUser")
    public static class GetLongForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getLongForUser, returning safe default: ".concat(message));
                return 1L;
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            String str;
            if (args != null) {
                try {
                    if (args.length > 0 && (str = (String) args[0]) != null && str.contains("feature_flag")) {
                        Slog.d(ISettingsProviderProxy.TAG, "Intercepting feature flag query: " + str + ", returning safe default");
                        return "true";
                    }
                } catch (Exception e) {
                    String message = e.getMessage();
                    if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                        throw e;
                    }
                    Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getString, returning safe default: ".concat(message));
                    return "true";
                }
            }
            String strGuestAndroidId = ISettingsProviderProxy.guestAndroidId(args);
            if (strGuestAndroidId != null) {
                return strGuestAndroidId;
            }
            String strHiddenSetting = ISettingsProviderProxy.hiddenSetting(args);
            return strHiddenSetting != null ? strHiddenSetting : method.invoke(who, args);
        }
    }

    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            String str;
            if (args != null) {
                try {
                    if (args.length > 0 && (str = (String) args[0]) != null && str.contains("feature_flag")) {
                        Slog.d(ISettingsProviderProxy.TAG, "Intercepting feature flag query: " + str + ", returning safe default");
                        return "true";
                    }
                } catch (Exception e) {
                    String message = e.getMessage();
                    if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                        throw e;
                    }
                    Slog.w(ISettingsProviderProxy.TAG, "UID mismatch in getStringForUser, returning safe default: ".concat(message));
                    return "true";
                }
            }
            String strGuestAndroidId = ISettingsProviderProxy.guestAndroidId(args);
            if (strGuestAndroidId != null) {
                return strGuestAndroidId;
            }
            String strHiddenSetting = ISettingsProviderProxy.hiddenSetting(args);
            return strHiddenSetting != null ? strHiddenSetting : method.invoke(who, args);
        }
    }

    public static String guestAndroidId(Object[] args) {
        String str;
        String appPackageName;
        String guestAndroidId;
        if (args == null) {
            return null;
        }
        try {
            int length = args.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    str = null;
                    break;
                }
                Object who = args[i];
                if ((who instanceof String) && "android_id".equals(who)) {
                    str = (String) who;
                    break;
                }
                i++;
            }
            if (str == null || (appPackageName = BActivityThread.getAppPackageName()) == null || GmsCore.isGoogleAppOrService(appPackageName) || (guestAndroidId = BlackBoxCore.getBActivityManager().getGuestAndroidId(BActivityThread.getUserId(), appPackageName)) == null || guestAndroidId.isEmpty()) {
                return null;
            }
            return guestAndroidId;
        } catch (Throwable unused) {
        }
    }

    public static String hiddenSetting(Object[] args) {
        String str;
        String appPackageName;
        if (args == null) {
            return null;
        }
        try {
            int length = args.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    str = null;
                    break;
                }
                Object who = args[i];
                if ((who instanceof String) && ("development_settings_enabled".equals(who) || "adb_enabled".equals(who))) {
                    str = (String) who;
                    break;
                }
                i++;
            }
            if (str == null || (appPackageName = BActivityThread.getAppPackageName()) == null || GmsCore.isGoogleAppOrService(appPackageName)) {
                return null;
            }
            return "0";
        } catch (Throwable unused) {
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
    }
}
