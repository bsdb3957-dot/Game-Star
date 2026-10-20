package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import black.android.content.pm.ICrossProfileAppsStubStatic;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.MethodReturnTypes;

public class ICrossProfileAppsProxy extends BinderInvocationStub {
    private static final String SERVICE = "crossprofileapps";

    public ICrossProfileAppsProxy() {
        super(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    public Object getWho() {
        return ((ICrossProfileAppsStubStatic) BlackReflection.create(ICrossProfileAppsStubStatic.class, null, false)).asInterface(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService(SERVICE);
    }

    @Override
    public Object invoke(Object who, Method method, Object[] args) {
        if ("getTargetUserProfiles".equals(method.getName())) {
            return Collections.EMPTY_LIST;
        }
        MethodParameterUtils.replaceFirstAppPkg(args);
        try {
            return super.invoke(who, method, args);
        } catch (SecurityException unused) {
            return List.class.isAssignableFrom(method.getReturnType()) ? Collections.EMPTY_LIST : MethodReturnTypes.defaultValue(method);
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
