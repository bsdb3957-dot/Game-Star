package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;
import android.os.IInterface;
import java.lang.reflect.Method;
import o5.g;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;

public class ISensorPrivacyManagerProxy extends BinderInvocationStub {
    public static final String TAG = "SensorPrivacyProxy";

    @ProxyMethod("isSensorPrivacyEnabled")
    public static class IsSensorPrivacyEnabled extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(ISensorPrivacyManagerProxy.TAG, "SensorPrivacy: isSensorPrivacyEnabled returning false");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isSensorPrivacyEnabledForProfile")
    public static class IsSensorPrivacyEnabledForProfile extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(ISensorPrivacyManagerProxy.TAG, "SensorPrivacy: isSensorPrivacyEnabledForProfile returning false");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isSensorPrivacyEnabledForUser")
    public static class IsSensorPrivacyEnabledForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(ISensorPrivacyManagerProxy.TAG, "SensorPrivacy: isSensorPrivacyEnabledForUser returning false");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("setSensorPrivacy")
    public static class SetSensorPrivacy extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(ISensorPrivacyManagerProxy.TAG, "SensorPrivacy: setSensorPrivacy allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setSensorPrivacyForProfile")
    public static class SetSensorPrivacyForProfile extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(ISensorPrivacyManagerProxy.TAG, "SensorPrivacy: setSensorPrivacyForProfile allowing");
            return method.invoke(who, args);
        }
    }

    public ISensorPrivacyManagerProxy() {
        super(g.a().getService("sensor_privacy"));
    }

    @Override
    public Object getWho() {
        Object objInvoke;
        try {
            try {
                objInvoke = Reflector.on("android.hardware.ISensorPrivacyManager$Stub").call("asInterface", g.a().getService("sensor_privacy"));
            } catch (Exception e) {
                Slog.d(TAG, "Failed Android 16+ path, trying alternative: " + e.getMessage());
                try {
                    objInvoke = Reflector.on("android.hardware.ISensorPrivacyManager").call("asInterface", g.a().getService("sensor_privacy"));
                } catch (Exception e) {
                    Slog.d(TAG, "Failed alternative path: " + e.getMessage());
                    try {
                        objInvoke = Class.forName("android.hardware.ISensorPrivacyManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, g.a().getService("sensor_privacy"));
                    } catch (Exception e) {
                        Slog.e(TAG, "All reflection paths failed for ISensorPrivacyManager", e);
                        return null;
                    }
                }
            }
            if (objInvoke != null) {
                Slog.d(TAG, "Successfully obtained ISensorPrivacyManager interface");
                return (IInterface) objInvoke;
            }
            Slog.e(TAG, "Reflection succeeded but returned null interface");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get ISensorPrivacyManager interface", e);
            return null;
        }
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService("sensor_privacy");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
