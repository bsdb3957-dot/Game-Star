package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class IAttributionSourceProxy extends ClassInvocationStub {
    public static final String TAG = "IAttributionSourceProxy";

    @ProxyMethod("AttributionSource")
    public static class AttributionSourceConstructor extends MethodHook {
        private Object createSafeAttributionSource(int i, String str) {
            Constructor<?> declaredConstructor;
            Class<?> cls = Integer.TYPE;
            try {
                Class<?> cls2 = Class.forName("android.content.AttributionSource");
                try {
                    try {
                        declaredConstructor = cls2.getDeclaredConstructor(cls, String.class, String.class);
                    } catch (NoSuchMethodException unused) {
                        declaredConstructor = cls2.getDeclaredConstructor(null);
                    }
                } catch (NoSuchMethodException unused2) {
                    declaredConstructor = cls2.getDeclaredConstructor(cls, String.class);
                }
                if (declaredConstructor == null) {
                    return null;
                }
                declaredConstructor.setAccessible(true);
                if (declaredConstructor.getParameterCount() == 3) {
                    return declaredConstructor.newInstance(Integer.valueOf(i), str, null);
                }
                return declaredConstructor.getParameterCount() == 2 ? declaredConstructor.newInstance(Integer.valueOf(i), str) : declaredConstructor.newInstance(null);
            } catch (Exception e) {
                Slog.e(IAttributionSourceProxy.TAG, "Failed to create safe AttributionSource: " + e.getMessage());;
                return null;
            }
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int hostUid = BlackBoxCore.getHostUid();
                String hostPkg = BlackBoxCore.getHostPkg();
                Slog.d(IAttributionSourceProxy.TAG, "Creating AttributionSource with UID: " + hostUid + ", package: " + hostPkg);
                Object objCreateSafeAttributionSource = createSafeAttributionSource(hostUid, hostPkg);
                return objCreateSafeAttributionSource != null ? objCreateSafeAttributionSource : method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IAttributionSourceProxy.TAG, "Error creating AttributionSource, using fallback: " + e.getMessage());
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            }
        }
    }

    @ProxyMethod("enforceCallingUid")
    public static class EnforceCallingUid extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(IAttributionSourceProxy.TAG, "Intercepting enforceCallingUid to prevent UID mismatch crashes");
                return null;
            } catch (Exception e) {
                Slog.w(IAttributionSourceProxy.TAG, "Error in enforceCallingUid hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("enforceCallingUidAndPid")
    public static class EnforceCallingUidAndPid extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(IAttributionSourceProxy.TAG, "Intercepting enforceCallingUidAndPid to prevent UID mismatch crashes");
                return null;
            } catch (Exception e) {
                Slog.w(IAttributionSourceProxy.TAG, "Error in enforceCallingUidAndPid hook: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("fromParcel")
    public static class FromParcel extends MethodHook {
        private Object createSafeAttributionSource(int i, String str) {
            try {
                Constructor<?> declaredConstructor = Class.forName("android.content.AttributionSource").getDeclaredConstructor(Integer.TYPE, String.class, String.class);
                declaredConstructor.setAccessible(true);
                return declaredConstructor.newInstance(Integer.valueOf(i), str, null);
            } catch (Exception e) {
                Slog.e(IAttributionSourceProxy.TAG, "Failed to create safe AttributionSource fallback: " + e.getMessage());;
                return null;
            }
        }

        private void fixAttributionSourceUid(Object who) {
            try {
                Method declaredMethod = who.getClass().getDeclaredMethod("setUid", Integer.TYPE);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(who, Integer.valueOf(BlackBoxCore.getHostUid()));
                Slog.d(IAttributionSourceProxy.TAG, "Fixed AttributionSource UID to: " + BlackBoxCore.getHostUid());
            } catch (Exception e) {
                Slog.w(IAttributionSourceProxy.TAG, "Could not fix AttributionSource UID: " + e.getMessage());
            }
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke == null) {
                    return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
                }
                fixAttributionSourceUid(objInvoke);
                return objInvoke;
            } catch (Exception e) {
                Slog.w(IAttributionSourceProxy.TAG, "Error in fromParcel, using fallback: " + e.getMessage());
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            }
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        Slog.d(TAG, "AttributionSource proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
