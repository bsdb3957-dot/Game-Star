package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class SystemLibraryProxy extends ClassInvocationStub {
    public static final String TAG = "SystemLibraryProxy";

    @ProxyMethod("load")
    public static class Load extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Object proxyInvocation = args[0];
                if (proxyInvocation instanceof String) {
                    String str = (String) proxyInvocation;
                    Slog.d(SystemLibraryProxy.TAG, "System: load called for: ".concat(str));
                    if (str.contains("libc++_shared.so") || str.contains("c++_shared")) {
                        Slog.d(SystemLibraryProxy.TAG, "System: Intercepting libc++_shared.so load, returning success");
                        return null;
                    }
                    if (str.contains("flutter") || str.contains("meemo")) {
                        Slog.d(SystemLibraryProxy.TAG, "System: Intercepting Flutter/Meemo library load, returning success");
                        return null;
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("loadLibrary")
    public static class LoadLibrary extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Object proxyInvocation = args[0];
                if (proxyInvocation instanceof String) {
                    String str = (String) proxyInvocation;
                    Slog.d(SystemLibraryProxy.TAG, "System: loadLibrary called for: ".concat(str));
                    if (str.equals("c++_shared") || str.contains("c++")) {
                        Slog.d(SystemLibraryProxy.TAG, "System: Intercepting c++ library load, returning success");
                        return null;
                    }
                    if (str.contains("flutter") || str.contains("meemo")) {
                        Slog.d(SystemLibraryProxy.TAG, "System: Intercepting Flutter/Meemo library load, returning success");
                        return null;
                    }
                }
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
