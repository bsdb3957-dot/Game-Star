package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class BrowserEngineProxy extends ClassInvocationStub {
    public static final String TAG = "BrowserEngineProxy";

    @ProxyMethod("killProcess")
    public static class KillProcess extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Slog.d(BrowserEngineProxy.TAG, "Process.killProcess() called for PID: " + ((Integer) args[0]).intValue());
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("start")
    public static class ProcessStart extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                Object objInvoke = method.invoke(who, args);
                if (args == null || args.length <= 0) {
                    return objInvoke;
                }
                Slog.d(BrowserEngineProxy.TAG, "Process.start() called with args count: " + args.length);
                return objInvoke;
            } catch (Exception e) {
                Slog.e(BrowserEngineProxy.TAG, "Process.start() failed: " + e.getMessage(), e);
                throw e;
            }
        }
    }

    @ProxyMethod("setArgV0")
    public static class SetArgV0 extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Slog.d(BrowserEngineProxy.TAG, "Process.setArgV0() called: " + ((String) args[0]));
            }
            return method.invoke(who, args);
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
    public void inject(Object who, Object proxyInvocation) {
    }
}
