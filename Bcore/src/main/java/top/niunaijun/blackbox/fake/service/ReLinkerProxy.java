package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ReLinkerProxy extends ClassInvocationStub {
    public static final String TAG = "ReLinkerProxy";

    @ProxyMethod("loadLibrary")
    public static class LoadLibrary extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(ReLinkerProxy.TAG, "ReLinker: loadLibrary called, intercepting to prevent MissingLibraryException");
            return null;
        }
    }

    @ProxyMethod("loadLibrary")
    public static class LoadLibraryWithAllParams extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || args.length <= 2) {
                return null;
            }
            Slog.d(ReLinkerProxy.TAG, "ReLinker: loadLibrary called for: " + ((String) args[1]) + " version: " + ((String) args[2]));
            return null;
        }
    }

    @ProxyMethod("loadLibrary")
    public static class LoadLibraryWithContext extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || args.length <= 1) {
                return null;
            }
            Slog.d(ReLinkerProxy.TAG, "ReLinker: loadLibrary called for: " + ((String) args[1]));
            return null;
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
