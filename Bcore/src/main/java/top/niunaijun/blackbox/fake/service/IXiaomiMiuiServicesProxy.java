package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.utils.Slog;

public class IXiaomiMiuiServicesProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiMiuiServicesProxy";

    @ProxyMethod("ForceDarkHelperStubImpl")
    public static class ForceDarkHelperStubImpl extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in Force Dark Helper Stub Implementation hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("HandWritingStubImpl")
    public static class HandWritingStubImpl extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in Hand Writing Stub Implementation hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("MiuiCameraCoveredManager")
    public static class MiuiCameraCoveredManager extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (NullPointerException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Attempt to invoke virtual method") || !message.contains("android.provider.MiuiSettings$SettingsCloudData$CloudData.getString")) {
                    throw e;
                }
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "MIUI Camera Manager NullPointerException on Xiaomi, creating safe fallback: " + (message));
                return IXiaomiMiuiServicesProxy.createSafeMiuiCameraManagerForXiaomi();
            } catch (SecurityException e) {
                String message2 = e.getMessage();
                if (message2 == null || !message2.contains("Calling uid") || !message2.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "MIUI Camera Manager UID mismatch on Xiaomi, creating safe fallback: " + (message2));
                return IXiaomiMiuiServicesProxy.createSafeMiuiCameraManagerForXiaomi();
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in MIUI Camera Manager hook: " + e.getMessage());
                return IXiaomiMiuiServicesProxy.createSafeMiuiCameraManagerForXiaomi();
            }
        }
    }

    @ProxyMethod("MiuiDownscaleImpl")
    public static class MiuiDownscaleImpl extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in MIUI Downscale Implementation hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("MiuiForceDarkConfig")
    public static class MiuiForceDarkConfig extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in MIUI Force Dark Config hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("MiuiMonitorThread")
    public static class MiuiMonitorThread extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in MIUI Monitor Thread hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("OplusOverScrollerExtImpl")
    public static class OplusOverScrollerExtImpl extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in Oplus Over Scroller Extension Implementation hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("OplusScrollToTopManager")
    public static class OplusScrollToTopManager extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IXiaomiMiuiServicesProxy.TAG, "Error in Oplus Scroll To Top Manager hook: " + e.getMessage());
                return null;
            }
        }
    }

    public static Object createSafeMiuiCameraManagerForXiaomi() {
        try {
            try {
                Constructor<?> declaredConstructor = Class.forName("android.provider.MiuiSettings$SettingsCloudData$CloudData").getDeclaredConstructor(null);
                declaredConstructor.setAccessible(true);
                Object objNewInstance = declaredConstructor.newInstance(null);
                Slog.d(TAG, "Created safe MiuiCameraManager for Xiaomi using default constructor");
                return objNewInstance;
            } catch (Exception e) {
                Slog.w(TAG, "Could not create safe MiuiCameraManager for Xiaomi: " + e.getMessage());
                return null;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error creating safe MiuiCameraManager for Xiaomi: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        if (!XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiMiuiServices proxy initialized for MIUI service crash prevention");
            return;
        }
        Slog.d(TAG, "IXiaomiMiuiServices proxy initialized for MIUI service crash prevention on " + XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
