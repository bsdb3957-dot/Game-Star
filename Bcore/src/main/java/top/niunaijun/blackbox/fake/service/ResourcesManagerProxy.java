package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class ResourcesManagerProxy extends ClassInvocationStub {
    private static final String RESOURCES_MANAGER_CLASS = "android.app.ResourcesManager";
    public static final String TAG = "ResourcesManagerProxy";

    @ProxyMethod("loadApkAssets")
    public static class LoadApkAssets extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            String str = (String) args[0];
            if (str == null || !(str.contains("resource-cache") || str.contains("@idmap") || str.contains(".frro") || str.contains("systemui") || str.contains("data@resource-cache@"))) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("loadOverlayFromPath")
    public static class LoadOverlayFromPath extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            String str = (String) args[0];
            if (str == null || !(str.contains("resource-cache") || str.contains("@idmap") || str.contains(".frro") || str.contains("systemui") || str.contains("data@resource-cache@"))) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    public ResourcesManagerProxy() {
        try {
            Class.forName(RESOURCES_MANAGER_CLASS);
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
