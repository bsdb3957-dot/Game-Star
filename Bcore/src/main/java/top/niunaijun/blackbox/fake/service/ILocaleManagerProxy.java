package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.LocaleList;
import black.android.app.ILocaleManagerStubStatic;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackreflection.BlackReflection;

public class ILocaleManagerProxy extends BinderInvocationStub {
    private static final String SERVICE = "locale";

    @ProxyMethod("getApplicationLocales")
    public static class GetApplicationLocales extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            return ILocaleManagerProxy.isGuestPackage(args) ? LocaleList.getEmptyLocaleList() : method.invoke(who, args);
        }
    }

    @ProxyMethod("getOverrideLocaleConfig")
    public static class GetOverrideLocaleConfig extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (ILocaleManagerProxy.isGuestPackage(args)) {
                return null;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setApplicationLocales")
    public static class SetApplicationLocales extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (ILocaleManagerProxy.isGuestPackage(args)) {
                return null;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setOverrideLocaleConfig")
    public static class SetOverrideLocaleConfig extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (ILocaleManagerProxy.isGuestPackage(args)) {
                return null;
            }
            return method.invoke(who, args);
        }
    }

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService(SERVICE));
    }

    public static boolean isGuestPackage(Object[] args) {
        return args != null && args.length > 0 && (args[0] instanceof String) && !BlackBoxCore.getHostPkg().equals(args[0]);
    }

    @Override
    public Object getWho() {
        return ((ILocaleManagerStubStatic) BlackReflection.create(ILocaleManagerStubStatic.class, null, false)).asInterface(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService(SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
