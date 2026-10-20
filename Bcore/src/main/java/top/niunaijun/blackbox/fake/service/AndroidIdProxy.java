package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class AndroidIdProxy extends ClassInvocationStub {
    public static final String TAG = "AndroidIdProxy";

    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AndroidIdProxy.TAG, "AndroidId: Handling get call");
                Object objInvoke = method.invoke(who, args);
                if (args != null && args.length > 0) {
                    Object proxyInvocation = args[0];
                    if (proxyInvocation instanceof String) {
                        String str = (String) proxyInvocation;
                        if (str.contains("android_id") || str.contains("ANDROID_ID") || str.contains("secure_id") || str.contains("device_id")) {
                            if (objInvoke == null || "0".equals(objInvoke.toString()) || "".equals(objInvoke.toString())) {
                                Slog.w(AndroidIdProxy.TAG, "AndroidId: Invalid Android ID get detected, returning mock ID");
                                return AndroidIdProxy.generateMockAndroidId();
                            }
                        }
                    }
                }
                return objInvoke;
            } catch (Exception e) {
                Slog.w(AndroidIdProxy.TAG, "AndroidId: Get error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("getAndroidId")
    public static class GetAndroidId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AndroidIdProxy.TAG, "AndroidId: Handling getAndroidId call");
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null && !"0".equals(objInvoke.toString()) && !"".equals(objInvoke.toString())) {
                    return objInvoke;
                }
                Slog.w(AndroidIdProxy.TAG, "AndroidId: Invalid Android ID detected, returning mock ID");
                return AndroidIdProxy.generateMockAndroidId();
            } catch (Exception e) {
                Slog.w(AndroidIdProxy.TAG, "AndroidId: GetAndroidId error, returning mock ID", e);
                return AndroidIdProxy.generateMockAndroidId();
            }
        }
    }

    @ProxyMethod("getLong")
    public static class GetLong extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AndroidIdProxy.TAG, "AndroidId: Handling getLong call");
                Object objInvoke = method.invoke(who, args);
                if (args != null && args.length > 0) {
                    Object proxyInvocation = args[0];
                    if (proxyInvocation instanceof String) {
                        String str = (String) proxyInvocation;
                        if (str.contains("android_id") || str.contains("ANDROID_ID") || str.contains("secure_id") || str.contains("device_id")) {
                            if (objInvoke == null || ((Number) objInvoke).longValue() == 0) {
                                Slog.w(AndroidIdProxy.TAG, "AndroidId: Invalid Android ID long detected, returning mock ID");
                                return AndroidIdProxy.generateMockAndroidIdLong();
                            }
                        }
                    }
                }
                return objInvoke;
            } catch (Exception e) {
                Slog.w(AndroidIdProxy.TAG, "AndroidId: GetLong error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AndroidIdProxy.TAG, "AndroidId: Handling getString call");
                Object objInvoke = method.invoke(who, args);
                if (args != null && args.length > 0) {
                    Object proxyInvocation = args[0];
                    if (proxyInvocation instanceof String) {
                        String str = (String) proxyInvocation;
                        if (str.contains("android_id") || str.contains("ANDROID_ID") || str.contains("secure_id") || str.contains("device_id")) {
                            if (objInvoke == null || "0".equals(objInvoke.toString()) || "".equals(objInvoke.toString())) {
                                Slog.w(AndroidIdProxy.TAG, "AndroidId: Invalid Android ID string detected, returning mock ID");
                                return AndroidIdProxy.generateMockAndroidId();
                            }
                        }
                    }
                }
                return objInvoke;
            } catch (Exception e) {
                Slog.w(AndroidIdProxy.TAG, "AndroidId: GetString error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("read")
    public static class Read extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AndroidIdProxy.TAG, "AndroidId: Handling read call");
                Object objInvoke = method.invoke(who, args);
                if (args != null && args.length > 0) {
                    Object proxyInvocation = args[0];
                    if (proxyInvocation instanceof String) {
                        String str = (String) proxyInvocation;
                        if (str.contains("android_id") || str.contains("ANDROID_ID") || str.contains("secure_id") || str.contains("device_id")) {
                            if (objInvoke == null || "0".equals(objInvoke.toString()) || "".equals(objInvoke.toString())) {
                                Slog.w(AndroidIdProxy.TAG, "AndroidId: Invalid Android ID read detected, returning mock ID");
                                return AndroidIdProxy.generateMockAndroidId();
                            }
                        }
                    }
                }
                return objInvoke;
            } catch (Exception e) {
                Slog.w(AndroidIdProxy.TAG, "AndroidId: Read error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    public static String generateMockAndroidId() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(Integer.toHexString((int) (Math.random() * 16.0d)));
        }
        String upperCase = sb.toString().toUpperCase();
        Slog.d(TAG, "AndroidId: Generated mock Android ID: " + upperCase);
        return upperCase;
    }

    public static Long generateMockAndroidIdLong() {
        long jRandom = (long) (Math.random() * 9.223372036854776E18d);
        Slog.d(TAG, "AndroidId: Generated mock Android ID long: " + jRandom);
        return Long.valueOf(jRandom);
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
