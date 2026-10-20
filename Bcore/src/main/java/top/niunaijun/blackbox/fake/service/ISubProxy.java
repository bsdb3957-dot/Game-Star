package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;
import android.os.IInterface;
import java.lang.reflect.Method;
import o5.g;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ISubProxy extends BinderInvocationStub {
    public static final String TAG = "ISubProxy";

    @ProxyMethod("getActiveSubIdList")
    public static class GetActiveSubIdList extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                return method.invoke(who, args);
            } catch (Throwable unused) {
                Slog.d(ISubProxy.TAG, "getActiveSubIdList denied, answering with no subscriptions");
                return new int[0];
            }
        }
    }

    public ISubProxy() {
        super(g.a().getService("isub"));
    }

    @Override
    public Object getWho() {
        try {
            IBinder service = g.a().getService("isub");
            if (service != null) {
                return (IInterface) Class.forName("com.android.internal.telephony.ISub$Stub").getMethod("asInterface", IBinder.class).invoke(null, service);
            }
            Slog.w(TAG, "no isub service on this device, leaving it alone");
            return null;
        } catch (Exception e) {
            Slog.w(TAG, "cannot wrap the isub service: " + e);
            return null;
        }
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService("isub");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
