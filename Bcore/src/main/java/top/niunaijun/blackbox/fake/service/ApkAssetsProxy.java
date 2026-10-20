package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class ApkAssetsProxy extends ClassInvocationStub {
    private static final String APK_ASSETS_CLASS = "android.content.res.ApkAssets";
    public static final String TAG = "ApkAssetsProxy";

    @ProxyMethod("loadOverlayFromPath")
    public static class LoadOverlayFromPath extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            String str = (String) args[0];
            if (str == null || !(str.contains("resource-cache") || str.contains("@idmap") || str.contains(".frro") || str.contains("systemui") || str.contains("data@resource-cache@"))) {
                return method.invoke(who, args);
            }
            throw new RuntimeException("Blocked problematic overlay path: ".concat(str));
        }
    }

    @ProxyMethod("nativeLoad")
    public static class NativeLoad extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            String str = (String) args[0];
            if (str == null || !(str.contains("resource-cache") || str.contains("@idmap") || str.contains(".frro") || str.contains("systemui") || str.contains("data@resource-cache@"))) {
                return method.invoke(who, args);
            }
            throw new RuntimeException("Blocked problematic native load path: ".concat(str));
        }
    }

    public ApkAssetsProxy() {
        try {
            Class.forName(APK_ASSETS_CLASS);
        } catch (ClassNotFoundException e) {
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
    public void onBindMethod() {
        super.onBindMethod();
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
    }
}
