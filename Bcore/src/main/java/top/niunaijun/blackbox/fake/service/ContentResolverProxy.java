package top.niunaijun.blackbox.fake.service;

import android.net.Uri;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ContentResolverProxy extends ClassInvocationStub {
    public static final String TAG = "ContentResolverProxy";

    @ProxyMethod("delete")
    public static class Delete extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Object proxyInvocation = args[0];
                if (proxyInvocation instanceof Uri) {
                    String string = ((Uri) proxyInvocation).toString();
                    if (string.contains("audio") || string.contains("media")) {
                        Slog.d(ContentResolverProxy.TAG, "ContentResolver: delete called for audio URI: " + (string));
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("insert")
    public static class Insert extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Object proxyInvocation = args[0];
                if (proxyInvocation instanceof Uri) {
                    String string = ((Uri) proxyInvocation).toString();
                    if (string.contains("audio") || string.contains("media")) {
                        Slog.d(ContentResolverProxy.TAG, "ContentResolver: insert called for audio URI: " + (string));
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("query")
    public static class Query extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Object proxyInvocation = args[0];
                if (proxyInvocation instanceof Uri) {
                    String string = ((Uri) proxyInvocation).toString();
                    if (string.contains("audio") || string.contains("media") || string.contains("content://media/external/audio") || string.contains("content://media/internal/audio") || string.contains("content://media/external/file") || string.contains("content://media/internal/file")) {
                        Slog.d(ContentResolverProxy.TAG, "ContentResolver: Allowing audio query: " + (string));
                        return method.invoke(who, args);
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("query")
    public static class QueryWithProjection extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 1) {
                Object proxyInvocation = args[0];
                if (proxyInvocation instanceof Uri) {
                    String string = ((Uri) proxyInvocation).toString();
                    if (string.contains("audio") || string.contains("media") || string.contains("content://media/external/audio") || string.contains("content://media/internal/audio") || string.contains("content://media/external/file") || string.contains("content://media/internal/file")) {
                        Slog.d(ContentResolverProxy.TAG, "ContentResolver: Allowing audio query with projection: " + (string));
                        return method.invoke(who, args);
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("update")
    public static class Update extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Object proxyInvocation = args[0];
                if (proxyInvocation instanceof Uri) {
                    String string = ((Uri) proxyInvocation).toString();
                    if (string.contains("audio") || string.contains("media")) {
                        Slog.d(ContentResolverProxy.TAG, "ContentResolver: update called for audio URI: " + (string));
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @Override
    public Object getWho() {
        return BlackBoxCore.getContext().getContentResolver();
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
    }
}
