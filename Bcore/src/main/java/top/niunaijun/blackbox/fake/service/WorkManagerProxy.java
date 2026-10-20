package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import java.lang.reflect.Method;
import java.util.Collections;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class WorkManagerProxy extends ClassInvocationStub {
    public static final String TAG = "WorkManagerProxy";

    @ProxyMethod("cancelAllWork")
    public static class CancelAllWork extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(WorkManagerProxy.TAG, "WorkManager: cancelAllWork() called");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(WorkManagerProxy.TAG, "WorkManager: cancelAllWork() failed, ignoring", e);
                return null;
            }
        }
    }

    @ProxyMethod("cancelWorkById")
    public static class CancelWorkById extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(WorkManagerProxy.TAG, "WorkManager: cancelWorkById() called");
                if (args != null && args.length > 0) {
                    Slog.d(WorkManagerProxy.TAG, "WorkManager: Cancelling work ID: " + ((String) args[0]));
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(WorkManagerProxy.TAG, "WorkManager: cancelWorkById() failed, ignoring", e);
                return null;
            }
        }
    }

    @ProxyMethod("enqueue")
    public static class Enqueue extends MethodHook {
        private Object createMockWorkResult() {
            return null;
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(WorkManagerProxy.TAG, "WorkManager: enqueue() called");
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            Slog.d(WorkManagerProxy.TAG, "WorkManager: args[" + i + "] = " + args[i].getClass().getSimpleName() + ": " + args[i]);
                        }
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(WorkManagerProxy.TAG, "WorkManager: enqueue() failed, returning mock result", e);
                return createMockWorkResult();
            }
        }
    }

    @ProxyMethod("enqueueUniquePeriodicWork")
    public static class EnqueueUniquePeriodicWork extends MethodHook {
        private Object createMockWorkResult() {
            return null;
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(WorkManagerProxy.TAG, "WorkManager: enqueueUniquePeriodicWork() called");
                if (args != null && args.length > 0) {
                    Slog.d(WorkManagerProxy.TAG, "WorkManager: Periodic work name: " + ((String) args[0]));
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(WorkManagerProxy.TAG, "WorkManager: enqueueUniquePeriodicWork() failed, returning mock result", e);
                return createMockWorkResult();
            }
        }
    }

    @ProxyMethod("enqueueUniqueWork")
    public static class EnqueueUniqueWork extends MethodHook {
        private Object createMockWorkResult() {
            return null;
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(WorkManagerProxy.TAG, "WorkManager: enqueueUniqueWork() called");
                if (args != null && args.length > 0) {
                    Slog.d(WorkManagerProxy.TAG, "WorkManager: Unique work name: " + ((String) args[0]));
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(WorkManagerProxy.TAG, "WorkManager: enqueueUniqueWork() failed, returning mock result", e);
                return createMockWorkResult();
            }
        }
    }

    @ProxyMethod("getWorkInfos")
    public static class GetWorkInfos extends MethodHook {
        private Object createEmptyWorkInfoList() {
            try {
                return Collections.EMPTY_LIST;
            } catch (Exception e) {
                Slog.w(WorkManagerProxy.TAG, "WorkManager: Failed to create empty work info list", e);
                return Collections.EMPTY_LIST;
            }
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(WorkManagerProxy.TAG, "WorkManager: getWorkInfos() called");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(WorkManagerProxy.TAG, "WorkManager: getWorkInfos() failed, returning empty list", e);
                return createEmptyWorkInfoList();
            }
        }
    }

    @Override
    public Object getWho() {
        try {
            Context context = BlackBoxCore.getContext();
            if (context != null) {
                return Class.forName("androidx.work.WorkManager").getMethod("getInstance", Context.class).invoke(null, context);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get WorkManager instance", e);
        }
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
