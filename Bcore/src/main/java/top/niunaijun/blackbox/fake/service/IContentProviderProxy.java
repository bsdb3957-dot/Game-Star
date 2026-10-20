package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.utils.Slog;

public class IContentProviderProxy extends ClassInvocationStub {
    public static final String TAG = "IContentProviderProxy";

    @ProxyMethod("call")
    public static class Call extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IContentProviderProxy.TAG, "UID mismatch in call method, returning safe default: ".concat(message));
                return null;
            } catch (Exception e) {
                Slog.w(IContentProviderProxy.TAG, "Error in call hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("delete")
    public static class Delete extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IContentProviderProxy.TAG, "Error in delete hook: " + e.getMessage());
                return 0;
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IContentProviderProxy.TAG, "UID mismatch in getString, returning safe default: ".concat(message));
                return "";
            } catch (Exception e) {
                Slog.w(IContentProviderProxy.TAG, "Error in getString hook: " + e.getMessage());
                return "";
            }
        }
    }

    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (SecurityException e) {
                String message = e.getMessage();
                if (message == null || !message.contains("Calling uid") || !message.contains("doesn't match source uid")) {
                    throw e;
                }
                Slog.w(IContentProviderProxy.TAG, "UID mismatch in getStringForUser, returning safe default: ".concat(message));
                return "";
            } catch (Exception e) {
                Slog.w(IContentProviderProxy.TAG, "Error in getStringForUser hook: " + e.getMessage());
                return "";
            }
        }
    }

    @ProxyMethod("insert")
    public static class Insert extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IContentProviderProxy.TAG, "Error in insert hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("query")
    public static class Query extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IContentProviderProxy.TAG, "Error in query hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("update")
    public static class Update extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IContentProviderProxy.TAG, "Error in update hook: " + e.getMessage());
                return 0;
            }
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        Slog.d(TAG, "IContentProvider proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
