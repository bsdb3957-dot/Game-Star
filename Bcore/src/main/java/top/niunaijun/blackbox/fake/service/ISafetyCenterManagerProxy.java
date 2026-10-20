package top.niunaijun.blackbox.fake.service;

import black.android.safetycenter.ISafetyCenterManagerStubStatic;
import java.lang.reflect.Method;
import o5.g;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackreflection.BlackReflection;

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
        super(g.a().getService(SERVICE));
    }

    @Override
    public Object getWho() {
        return ((ISafetyCenterManagerStubStatic) BlackReflection.create(ISafetyCenterManagerStubStatic.class, null, false)).asInterface(g.a().getService(SERVICE));
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
