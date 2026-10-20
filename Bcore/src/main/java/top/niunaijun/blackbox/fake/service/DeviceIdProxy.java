package top.niunaijun.blackbox.fake.service;

import androidx.fragment.app.z0;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class DeviceIdProxy extends ClassInvocationStub {
    public static final String TAG = "DeviceIdProxy";

    @ProxyMethod("generateDeviceId")
    public static class GenerateDeviceId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(DeviceIdProxy.TAG, "GenerateDeviceId error, returning default device ID: " + e.getMessage());
                return "generated_device_id_" + System.currentTimeMillis();
            }
        }
    }

    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                if (who != null) {
                    return method.invoke(who, args);
                }
                Slog.w(DeviceIdProxy.TAG, "GetDeviceId called on null object, returning default device ID");
                return "default_device_id";
            } catch (Exception e) {
                z0.o("GetDeviceId error, returning default device ID: ", e, DeviceIdProxy.TAG);
                return "default_device_id";
            }
        }
    }

    @ProxyMethod("isValidDeviceId")
    public static class IsValidDeviceId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                if (who != null) {
                    return method.invoke(who, args);
                }
                Slog.w(DeviceIdProxy.TAG, "IsValidDeviceId called on null object, returning true");
                return Boolean.TRUE;
            } catch (Exception e) {
                z0.o("IsValidDeviceId error, returning true: ", e, DeviceIdProxy.TAG);
                return Boolean.TRUE;
            }
        }
    }

    @ProxyMethod("retrieveDeviceId")
    public static class RetrieveDeviceId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                if (who != null) {
                    return method.invoke(who, args);
                }
                Slog.w(DeviceIdProxy.TAG, "RetrieveDeviceId called on null object, returning default device ID");
                return "retrieved_device_id";
            } catch (Exception e) {
                z0.o("RetrieveDeviceId error, returning default device ID: ", e, DeviceIdProxy.TAG);
                return "retrieved_device_id";
            }
        }
    }

    @ProxyMethod("setDeviceId")
    public static class SetDeviceId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                if (who != null) {
                    return method.invoke(who, args);
                }
                Slog.w(DeviceIdProxy.TAG, "SetDeviceId called on null object, ignoring");
                return null;
            } catch (Exception e) {
                z0.o("SetDeviceId error, ignoring: ", e, DeviceIdProxy.TAG);
                return null;
            }
        }
    }

    @ProxyMethod("storeDeviceId")
    public static class StoreDeviceId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                if (who != null) {
                    return method.invoke(who, args);
                }
                Slog.w(DeviceIdProxy.TAG, "StoreDeviceId called on null object, ignoring");
                return null;
            } catch (Exception e) {
                z0.o("StoreDeviceId error, ignoring: ", e, DeviceIdProxy.TAG);
                return null;
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
