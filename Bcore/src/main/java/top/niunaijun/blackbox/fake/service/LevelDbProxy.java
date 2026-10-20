package top.niunaijun.blackbox.fake.service;

import androidx.fragment.app.z0;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class LevelDbProxy extends ClassInvocationStub {
    public static final String TAG = "LevelDbProxy";

    @ProxyMethod("delete")
    public static class Delete extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (who == null) {
                Slog.w(LevelDbProxy.TAG, "LevelDB delete called on null database, returning false");
                return Boolean.FALSE;
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                z0.o("LevelDB delete error, returning false: ", e, LevelDbProxy.TAG);
                return Boolean.FALSE;
            }
        }
    }

    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (who == null) {
                Slog.w(LevelDbProxy.TAG, "LevelDB get called on null database, returning null");
                return null;
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                z0.o("LevelDB get error, returning null: ", e, LevelDbProxy.TAG);
                return null;
            }
        }
    }

    @ProxyMethod("nativeOpen")
    public static class NativeOpen extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || !(message.contains("lock") || message.contains("IO error"))) {
                    throw e;
                }
                Slog.w(LevelDbProxy.TAG, "LevelDB native lock/IO error detected, returning null: ".concat(message));
                return null;
            }
        }
    }

    @ProxyMethod("open")
    public static class Open extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Exception {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("lock") && message.contains("Try again")) {
                    Slog.w(LevelDbProxy.TAG, "LevelDB lock error detected, returning null: ".concat(message));
                    return null;
                }
                if (message == null || !message.contains("IO error")) {
                    throw e;
                }
                Slog.w(LevelDbProxy.TAG, "LevelDB IO error detected, returning null: ".concat(message));
                return null;
            }
        }
    }

    @ProxyMethod("put")
    public static class Put extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (who == null) {
                Slog.w(LevelDbProxy.TAG, "LevelDB put called on null database, returning false");
                return Boolean.FALSE;
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                z0.o("LevelDB put error, returning false: ", e, LevelDbProxy.TAG);
                return Boolean.FALSE;
            }
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
