package top.niunaijun.blackbox.fake.service;

import android.os.Binder;
import android.os.IInterface;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import o5.g;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.MethodReturnTypes;
import top.niunaijun.blackbox.utils.Slog;

public class StatsManagerStub implements IInjectHook {
    private static final String DESCRIPTOR = "android.os.IStatsManagerService";
    private static final String SERVICE = "statsmanager";
    private static final String TAG = "StatsManagerStub";

    public static Object lambda$injectHook$0(Binder binder, Object who, Method method, Object[] args) {
        String name = method.getName();
        boolean z6 = false;
        switch (name) {
            case "toString":
                return TAG;
            case "asBinder":
                return binder;
            case "equals":
                if (args != null && args.length == 1 && who == args[0]) {
                    z6 = true;
                }
                return Boolean.valueOf(z6);
            case "hashCode":
                return Integer.valueOf(System.identityHashCode(who));
            default:
                return MethodReturnTypes.defaultValue(method);
        }
    }

    @Override
    public void injectHook() {
        try {
            if (g.a().getService(SERVICE) != null) {
                return;
            }
            Class<?> cls = Class.forName(DESCRIPTOR);
            final Binder binder = new Binder();
            binder.attachInterface((IInterface) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() { // from class: top.niunaijun.blackbox.fake.service.a
                @Override
                public final Object invoke(Object who, Method method, Object[] args) {
                    return StatsManagerStub.lambda$injectHook$0(binder, who, method, args);
                }
            }), DESCRIPTOR);
            g.a().sCache().put(SERVICE, binder);
        } catch (Throwable th) {
            Slog.w(TAG, "could not install a stand-in statsmanager: " + th);
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
