package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import black.android.safetycenter.ISafetyCenterManagerStubStatic;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class ISafetyCenterManagerProxy extends BinderInvocationStub {
    private static final String SERVICE = "safety_center";

    @ProxyMethod("isSafetyCenterEnabled")
    public static class IsSafetyCenterEnabled extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            return Boolean.FALSE;
        }
    }

    public ISafetyCenterManagerProxy() {
        super(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    public Object getWho() {
        return ((ISafetyCenterManagerStubStatic) BlackReflection.create(ISafetyCenterManagerStubStatic.class, null, false)).asInterface(BRServiceManager.get().getService(SERVICE));
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
