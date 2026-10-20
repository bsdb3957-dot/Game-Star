package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class AuthenticationProxy extends ClassInvocationStub {
    public static final String TAG = "AuthenticationProxy";

    @ProxyMethod("getAccessToken")
    public static class GetAccessToken extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling getAccessToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: GetAccessToken error, returning mock token", e);
                return "mock_access_token_" + System.currentTimeMillis();
            }
        }
    }

    @ProxyMethod("getCurrentUser")
    public static class GetCurrentUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling getCurrentUser call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: GetCurrentUser error, returning mock user", e);
                return AuthenticationProxy.createMockUser();
            }
        }
    }

    @ProxyMethod("isLoggedIn")
    public static class IsLoggedIn extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling isLoggedIn call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: IsLoggedIn error, returning true", e);
                return Boolean.TRUE;
            }
        }
    }

    @ProxyMethod("isSignedIn")
    public static class IsSignedIn extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling isSignedIn call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: IsSignedIn error, returning true", e);
                return Boolean.TRUE;
            }
        }
    }

    @ProxyMethod("login")
    public static class Login extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling login call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: Login error, returning success", e);
                return AuthenticationProxy.createMockLoginResult();
            }
        }
    }

    @ProxyMethod("logout")
    public static class Logout extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling logout call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: Logout error, returning success", e);
                return null;
            }
        }
    }

    @ProxyMethod("refreshToken")
    public static class RefreshToken extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling refreshToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: RefreshToken error, returning mock token", e);
                return "mock_refresh_token_" + System.currentTimeMillis();
            }
        }
    }

    @ProxyMethod("signIn")
    public static class SignIn extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling signIn call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: SignIn error, returning success", e);
                return AuthenticationProxy.createMockSignInResult();
            }
        }
    }

    @ProxyMethod("signOut")
    public static class SignOut extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling signOut call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: SignOut error, returning success", e);
                return null;
            }
        }
    }

    @ProxyMethod("validateToken")
    public static class ValidateToken extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(AuthenticationProxy.TAG, "Authentication: Handling validateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(AuthenticationProxy.TAG, "Authentication: ValidateToken error, returning true", e);
                return Boolean.TRUE;
            }
        }
    }

    public static Object createMockLoginResult() {
        try {
            return Class.forName("android.os.Bundle").newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock login result", e);
            return null;
        }
    }

    public static Object createMockSignInResult() {
        try {
            return Class.forName("android.os.Bundle").newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock sign in result", e);
            return null;
        }
    }

    public static Object createMockUser() {
        try {
            return Class.forName("android.os.Bundle").newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock user", e);
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
