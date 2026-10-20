package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import black.android.app.backup.IBackupManagerStubStatic;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodReturnTypes;

public class IBackupManagerProxy extends BinderInvocationStub {
    private static final String SERVICE = "backup";

    @ProxyMethod("isBackupServiceActive")
    public static class IsBackupServiceActive extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            return Boolean.FALSE;
        }
    }

    public IBackupManagerProxy() {
        super(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    public Object getWho() {
        return ((IBackupManagerStubStatic) BlackReflection.create(IBackupManagerStubStatic.class, null, false)).asInterface(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService(SERVICE);
    }

    @Override
    public Object invoke(Object who, Method method, Object[] args) {
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
