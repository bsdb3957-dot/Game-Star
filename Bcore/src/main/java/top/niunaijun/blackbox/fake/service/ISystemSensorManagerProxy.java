package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.utils.Slog;

public class ISystemSensorManagerProxy extends ClassInvocationStub {
    public static final String TAG = "ISystemSensorManagerProxy";

    @ProxyMethod("SystemSensorManager")
    public static class Constructor extends MethodHook {
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
                Slog.w(ISystemSensorManagerProxy.TAG, "UID mismatch in SystemSensorManager constructor, creating safe fallback: ".concat(message));
                return ISystemSensorManagerProxy.createSafeSensorManager();
            } catch (Exception e) {
                Slog.w(ISystemSensorManagerProxy.TAG, new StringBuilder("Error in SystemSensorManager constructor hook: ").append(e.getMessage()).toString());
                return ISystemSensorManagerProxy.createSafeSensorManager();
            }
        }
    }

    @ProxyMethod("parsePackageList")
    public static class ParsePackageList extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(ISystemSensorManagerProxy.TAG, "Error in parsePackageList hook: " + e.getMessage());
                return new ArrayList();
            }
        }
    }

    public static Object createSafeSensorManager() {
        try {
            try {
                java.lang.reflect.Constructor<?> declaredConstructor = Class.forName("android.hardware.SystemSensorManager").getDeclaredConstructor(null);
                declaredConstructor.setAccessible(true);
                Object objNewInstance = declaredConstructor.newInstance(null);
                Slog.d(TAG, "Created safe SystemSensorManager using default constructor");
                return objNewInstance;
            } catch (Exception e) {
                Slog.w(TAG, "Could not create safe SystemSensorManager: " + e.getMessage());
                return null;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error creating safe SystemSensorManager: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        Slog.d(TAG, "ISystemSensorManager proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
