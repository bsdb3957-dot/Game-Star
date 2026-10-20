package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class FeatureFlagUtilsProxy extends ClassInvocationStub {
    public static final String TAG = "FeatureFlagUtilsProxy";

    @ProxyMethod("getBoolean")
    public static class GetBoolean extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(FeatureFlagUtilsProxy.TAG, "UID mismatch in FeatureFlagUtils.getBoolean, returning safe default: ".concat(message));
                return Boolean.TRUE;
            }
        }
    }

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
                Slog.w(FeatureFlagUtilsProxy.TAG, "UID mismatch in FeatureFlagUtils.getFloat, returning safe default: ".concat(message));
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
                Slog.w(FeatureFlagUtilsProxy.TAG, "UID mismatch in FeatureFlagUtils.getInt, returning safe default: ".concat(message));
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
                Slog.w(FeatureFlagUtilsProxy.TAG, "UID mismatch in FeatureFlagUtils.getLong, returning safe default: ".concat(message));
                return 1L;
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(FeatureFlagUtilsProxy.TAG, "UID mismatch in FeatureFlagUtils.getString, returning safe default: ".concat(message));
                return "true";
            }
        }
    }

    @ProxyMethod("isEnabled")
    public static class IsEnabled extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(FeatureFlagUtilsProxy.TAG, "UID mismatch in FeatureFlagUtils.isEnabled, returning safe default: ".concat(message));
                return Boolean.TRUE;
            }
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
