package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.utils.Slog;

public class IXiaomiAttributionSourceProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiAttributionSourceProxy";

    @ProxyMethod("AttributionSource")
    public static class AttributionSourceConstructor extends MethodHook {
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
                Slog.w(IXiaomiAttributionSourceProxy.TAG, "Xiaomi UID mismatch in AttributionSource constructor, creating safe fallback: ".concat(message));
                return AttributionSourceUtils.createSafeAttributionSource();
            } catch (Exception e) {
                Slog.w(IXiaomiAttributionSourceProxy.TAG, "Error in Xiaomi AttributionSource constructor hook: " + e.getMessage());
                return AttributionSourceUtils.createSafeAttributionSource();
            }
        }
    }

    @ProxyMethod("enforceCallingUid")
    public static class EnforceCallingUid extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IXiaomiAttributionSourceProxy.TAG, "Xiaomi enforceCallingUid bypassed to prevent UID mismatch crashes");
            return null;
        }
    }

    @ProxyMethod("enforceCallingUidAndPid")
    public static class EnforceCallingUidAndPid extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IXiaomiAttributionSourceProxy.TAG, "Xiaomi enforceCallingUidAndPid bypassed to prevent UID mismatch crashes");
            return null;
        }
    }

    @ProxyMethod("fromParcel")
    public static class FromParcel extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke == null) {
                    return objInvoke;
                }
                AttributionSourceUtils.fixAttributionSourceUid(objInvoke);
                return objInvoke;
            } catch (Exception e) {
                Slog.w(IXiaomiAttributionSourceProxy.TAG, "Error in Xiaomi fromParcel hook: " + e.getMessage());
                return AttributionSourceUtils.createSafeAttributionSource();
            }
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        if (!XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiAttributionSource proxy initialized for MIUI UID mismatch prevention");
            return;
        }
        Slog.d(TAG, "IXiaomiAttributionSource proxy initialized for MIUI UID mismatch prevention on " + XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
