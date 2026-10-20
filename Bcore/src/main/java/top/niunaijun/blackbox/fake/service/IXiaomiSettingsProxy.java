package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.utils.Slog;

public class IXiaomiSettingsProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiSettingsProxy";

    @ProxyMethod("getInt")
    public static class GetInt extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IXiaomiSettingsProxy.TAG, "Xiaomi UID mismatch in getInt, returning safe default: ".concat(message));
                return Integer.valueOf(IXiaomiSettingsProxy.getSafeDefaultIntForXiaomiSettings());
            } catch (Exception e) {
                Slog.w(IXiaomiSettingsProxy.TAG, new StringBuilder("Error in Xiaomi getInt hook: ").append(e.getMessage()).toString());
                return Integer.valueOf(IXiaomiSettingsProxy.getSafeDefaultIntForXiaomiSettings());
            }
        }
    }

    @ProxyMethod("getIntForUser")
    public static class GetIntForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IXiaomiSettingsProxy.TAG, "Xiaomi UID mismatch in getIntForUser, returning safe default: ".concat(message));
                return Integer.valueOf(IXiaomiSettingsProxy.getSafeDefaultIntForXiaomiSettings());
            } catch (Exception e) {
                Slog.w(IXiaomiSettingsProxy.TAG, new StringBuilder("Error in Xiaomi getIntForUser hook: ").append(e.getMessage()).toString());
                return Integer.valueOf(IXiaomiSettingsProxy.getSafeDefaultIntForXiaomiSettings());
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IXiaomiSettingsProxy.TAG, "Xiaomi UID mismatch in getString, returning safe default: ".concat(message));
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            } catch (Exception e) {
                Slog.w(IXiaomiSettingsProxy.TAG, new StringBuilder("Error in Xiaomi getString hook: ").append(e.getMessage()).toString());
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            }
        }
    }

    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IXiaomiSettingsProxy.TAG, "Xiaomi UID mismatch in getStringForUser, returning safe default: ".concat(message));
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            } catch (Exception e) {
                Slog.w(IXiaomiSettingsProxy.TAG, new StringBuilder("Error in Xiaomi getStringForUser hook: ").append(e.getMessage()).toString());
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            }
        }
    }

    @ProxyMethod("getString")
    public static class GlobalGetString extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IXiaomiSettingsProxy.TAG, "Xiaomi UID mismatch in Global.getString, returning safe default: ".concat(message));
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            } catch (Exception e) {
                Slog.w(IXiaomiSettingsProxy.TAG, new StringBuilder("Error in Xiaomi Global.getString hook: ").append(e.getMessage()).toString());
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            }
        }
    }

    @ProxyMethod("getStringForUser")
    public static class GlobalGetStringForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IXiaomiSettingsProxy.TAG, "Xiaomi UID mismatch in Global.getStringForUser, returning safe default: ".concat(message));
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            } catch (Exception e) {
                Slog.w(IXiaomiSettingsProxy.TAG, new StringBuilder("Error in Xiaomi Global.getStringForUser hook: ").append(e.getMessage()).toString());
                return IXiaomiSettingsProxy.getSafeDefaultForXiaomiSettings();
            }
        }
    }

    public static String getSafeDefaultForXiaomiSettings() {
        return "";
    }

    public static int getSafeDefaultIntForXiaomiSettings() {
        return 0;
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        if (!XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiSettings proxy initialized for MIUI Settings UID mismatch prevention");
            return;
        }
        Slog.d(TAG, "IXiaomiSettings proxy initialized for MIUI Settings UID mismatch prevention on " + XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
