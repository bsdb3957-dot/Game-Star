package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.IBinder;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";

    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(GmsProxy.TAG, "GmsProxy: Handling authenticate call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(GmsProxy.TAG, "GmsProxy: Authentication error, returning success", e);
                return GmsProxy.createMockAuthResult();
            }
        }
    }

    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(GmsProxy.TAG, "GmsProxy: Handling clearToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(GmsProxy.TAG, "GmsProxy: ClearToken error, ignoring", e);
                return null;
            }
        }
    }

    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(GmsProxy.TAG, "GmsProxy: Handling getAccount call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(GmsProxy.TAG, "GmsProxy: GetAccount error, returning null", e);
                return null;
            }
        }
    }

    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null) {
                try {
                    if (args.length > 0 && GmsCore.GMS_PKG.equals((String) args[0])) {
                        args[0] = BlackBoxCore.getHostPkg();
                        Slog.d(GmsProxy.TAG, "GmsProxy: Fixed calling package from com.google.android.gms to " + BlackBoxCore.getHostPkg());
                    }
                } catch (Exception e) {
                    Slog.e(GmsProxy.TAG, "GmsProxy: Error in getService", e);
                    return null;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(GmsProxy.TAG, "GmsProxy: Error in getServiceBroker", e);
                return null;
            }
        }
    }

    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(GmsProxy.TAG, "GmsProxy: Handling getToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(GmsProxy.TAG, "GmsProxy: GetToken error, returning mock token", e);
                return "mock_gms_token_" + System.currentTimeMillis();
            }
        }
    }

    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(GmsProxy.TAG, "GmsProxy: Handling invalidateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(GmsProxy.TAG, "GmsProxy: InvalidateToken error, ignoring", e);
                return null;
            }
        }
    }

    public GmsProxy() {
        super(BRServiceManager.get().getService("gms"));
    }

    public static Object createMockAuthResult() {
        try {
            return Class.forName("android.os.Bundle").newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock auth result", e);
            return null;
        }
    }

    @Override
    public Object getWho() {
        IBinder service = BRServiceManager.get().getService("gms");
        if (service == null) {
            Slog.e(TAG, "Failed to get gms service binder");
            return null;
        }
        try {
            Object objInvoke = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub").getMethod("asInterface", IBinder.class).invoke(null, service);
            if (objInvoke != null) {
                Slog.d(TAG, "Successfully obtained IGmsServiceBroker interface");
                return objInvoke;
            }
            Slog.e(TAG, "Reflection succeeded but returned null interface");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IGmsServiceBroker interface", e);
            return null;
        }
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService("gms");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
