package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import black.android.content.IClipboardStubStatic;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.MethodReturnTypes;
import top.niunaijun.blackreflection.BlackReflection;

public class IClipboardProxy extends BinderInvocationStub {
    public IClipboardProxy() {
        super(BRServiceManager.get().getService("clipboard"));
    }

    @Override
    public Object getWho() {
        return ((IClipboardStubStatic) BlackReflection.create(IClipboardStubStatic.class, null, false)).asInterface(BRServiceManager.get().getService("clipboard"));
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService("clipboard");
    }

    @Override
    public Object invoke(Object who, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        try {
            return super.invoke(who, method, args);
        } catch (SecurityException unused) {
            return MethodReturnTypes.defaultValue(method);
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
