package top.niunaijun.blackbox.fake.service;

import androidx.fragment.app.z0;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.utils.Slog;

public class ISettingsSystemProxy extends ClassInvocationStub {
    public static final String TAG = "ISettingsSystemProxy";

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
                Slog.w(ISettingsSystemProxy.TAG, "UID mismatch in getString, returning safe default: ".concat(message));
                return "";
            } catch (Exception e) {
                z0.o("Error in getString hook: ", e, ISettingsSystemProxy.TAG);
                return "";
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
                Slog.w(ISettingsSystemProxy.TAG, "UID mismatch in getStringForUser, returning safe default: ".concat(message));
                return "";
            } catch (Exception e) {
                z0.o("Error in getStringForUser hook: ", e, ISettingsSystemProxy.TAG);
                return "";
            }
        }
    }

    @ProxyMethod("putString")
    public static class PutString extends MethodHook {
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
                Slog.w(ISettingsSystemProxy.TAG, "UID mismatch in putString, returning false: ".concat(message));
                return Boolean.FALSE;
            } catch (Exception e) {
                z0.o("Error in putString hook: ", e, ISettingsSystemProxy.TAG);
                return Boolean.FALSE;
            }
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        Slog.d(TAG, "ISettingsSystem proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
