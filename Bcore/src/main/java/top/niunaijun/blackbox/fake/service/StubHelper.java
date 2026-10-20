package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;

import top.niunaijun.blackbox.utils.Slog;

public class StubHelper {
    private static final String TAG = "StubHelper";

    private static Method findAsInterfaceMethod(Class<?> facadeClass) {
        try {
            return facadeClass.getMethod("asInterface", IBinder.class);
        } catch (NoSuchMethodException e) {
            for (Method candidate : facadeClass.getMethods()) {
                if (candidate.getName().equals("asInterface") && candidate.getParameterTypes().length == 1) {
                    return candidate;
                }
            }
            try {
                return facadeClass.getMethod("asInterface", Object.class);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }

    public static Object getServiceInterface(String serviceName, String staticStubClass, String interfaceClass) {
        try {
            Class<?> facadeClass = Class.forName(staticStubClass);
            Object facade = facadeClass.getMethod("get").invoke(null);
            Method asInterface = findAsInterfaceMethod(facadeClass);
            if (asInterface != null) {
                return asInterface.invoke(facade, BRServiceManager.get().getService(serviceName));
            }
        } catch (Exception e) {
        }
        try {
            return Class.forName(interfaceClass).getMethod("asInterface", IBinder.class)
                    .invoke(null, BRServiceManager.get().getService(serviceName));
        } catch (Exception e) {
            Slog.w(TAG, "getServiceInterface failed for " + interfaceClass + ": " + e.getMessage());
            return null;
        }
    }

    public static Object getServiceInterface(String serviceName, String staticStubClass, String interfaceClass, IBinder binder) {
        try {
            Class<?> facadeClass = Class.forName(staticStubClass);
            Object facade = facadeClass.getMethod("get").invoke(null);
            Method asInterface = findAsInterfaceMethod(facadeClass);
            if (asInterface != null) {
                return asInterface.invoke(facade, binder != null ? binder : BRServiceManager.get().getService(serviceName));
            }
        } catch (Exception e) {
        }
        if (binder == null) {
            binder = BRServiceManager.get().getService(serviceName);
            if (binder == null) {
                return null;
            }
        }
        try {
            return Class.forName(interfaceClass).getMethod("asInterface", IBinder.class).invoke(null, binder);
        } catch (Exception e) {
            Slog.w(TAG, "getServiceInterface failed for " + interfaceClass + ": " + e.getMessage());
            return null;
        }
    }
}
