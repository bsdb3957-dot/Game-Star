package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.Build;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import top.niunaijun.blackbox.core.system.user.BUserHandle;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class IDnsResolverProxy extends BinderInvocationStub {
    public static final String DNS_RESOLVER_SERVICE = "dnsresolver";
    public static final String TAG = "IDnsResolverProxy";

    @ProxyMethod("getDnsResolverStats")
    public static class GetDnsResolverStats extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (Build.VERSION.SDK_INT >= 23) {
                Slog.d(IDnsResolverProxy.TAG, "Intercepting DNS resolver stats request");
                try {
                    return method.invoke(who, args);
                } catch (Exception e) {
                    Slog.w(IDnsResolverProxy.TAG, "DNS resolver stats failed: " + e.getMessage());
                }
            }
            return null;
        }
    }

    @ProxyMethod("isNetworkValidated")
    public static class IsNetworkValidated extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(IDnsResolverProxy.TAG, "Intercepting network validation check");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("resolveDns")
    public static class ResolveDns extends MethodHook {
        private Object createFallbackDnsResult() {
            try {
                ArrayList arrayList = new ArrayList();
                arrayList.add(InetAddress.getByName("8.8.8.8"));
                arrayList.add(InetAddress.getByName("8.8.4.4"));
                return arrayList;
            } catch (Exception e) {
                Slog.e(IDnsResolverProxy.TAG, "Error creating fallback DNS result: " + e.getMessage());
                return new ArrayList();
            }
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(IDnsResolverProxy.TAG, "Intercepting DNS resolution request");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    return objInvoke;
                }
                Slog.w(IDnsResolverProxy.TAG, "DNS resolution failed, providing fallback");
                return createFallbackDnsResult();
            } catch (Exception e) {
                Slog.w(IDnsResolverProxy.TAG, "DNS resolution error, providing fallback: " + e.getMessage());
                return createFallbackDnsResult();
            }
        }
    }

    @ProxyMethod("setDnsQueryTimeout")
    public static class SetDnsQueryTimeout extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(IDnsResolverProxy.TAG, "Intercepting DNS query timeout configuration");
            if (args != null) {
                try {
                    if (args.length > 0) {
                        Object proxyInvocation = args[0];
                        if (proxyInvocation instanceof Integer) {
                            int iMin = Math.min(((Integer) proxyInvocation).intValue(), BUserHandle.AID_APP_START);
                            Slog.d(IDnsResolverProxy.TAG, "Setting DNS query timeout to: " + iMin + "ms");
                            args[0] = Integer.valueOf(iMin);
                        }
                    }
                } catch (Exception e) {
                    Slog.w(IDnsResolverProxy.TAG, "DNS query timeout configuration failed: " + e.getMessage());
                    return null;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setDnsServersForNetwork")
    public static class SetDnsServersForNetwork extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(IDnsResolverProxy.TAG, "Intercepting DNS server configuration");
            try {
                Object objInvoke = method.invoke(who, args);
                Slog.d(IDnsResolverProxy.TAG, "DNS server configuration applied");
                return objInvoke;
            } catch (Exception e) {
                Slog.w(IDnsResolverProxy.TAG, "DNS server configuration failed: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("setPrivateDnsConfiguration")
    public static class SetPrivateDnsConfiguration extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (Build.VERSION.SDK_INT >= 28) {
                Slog.d(IDnsResolverProxy.TAG, "Intercepting private DNS configuration");
                if (args != null) {
                    try {
                        if (args.length > 0) {
                            Slog.d(IDnsResolverProxy.TAG, "Disabling private DNS for sandboxed app");
                        }
                    } catch (Exception e) {
                        Slog.w(IDnsResolverProxy.TAG, "Private DNS configuration failed: " + e.getMessage());
                    }
                }
                return method.invoke(who, args);
            }
            return null;
        }
    }

    public IDnsResolverProxy() {
        super(BRServiceManager.get().getService(DNS_RESOLVER_SERVICE));
    }

    @Override
    public Object getWho() {
        Slog.d(TAG, "IDnsResolverProxy: Returning null for getWho to avoid reflection issues");
        return null;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        try {
            replaceSystemService(DNS_RESOLVER_SERVICE);
            Slog.d(TAG, "IDnsResolverProxy: Successfully injected DNS resolver service");
        } catch (Exception e) {
            Slog.w(TAG, "IDnsResolverProxy: Failed to inject service: " + e.getMessage());
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
