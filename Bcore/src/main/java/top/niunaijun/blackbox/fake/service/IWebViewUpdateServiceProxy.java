package top.niunaijun.blackbox.fake.service;

import android.content.pm.PackageInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class IWebViewUpdateServiceProxy extends ClassInvocationStub {
    public static final String TAG = "IWebViewUpdateServiceProxy";

    @ProxyMethod("disableWebViewPackage")
    public static class DisableWebViewPackage extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: disableWebViewPackage called for: " + ((String) args[0]));
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to disable WebView package", e);
                return Boolean.TRUE;
            }
        }
    }

    @ProxyMethod("enableWebViewPackage")
    public static class EnableWebViewPackage extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: enableWebViewPackage called for: " + ((String) args[0]));
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to enable WebView package", e);
                return Boolean.TRUE;
            }
        }
    }

    @ProxyMethod("getCurrentWebViewPackage")
    public static class GetCurrentWebViewPackage extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: getCurrentWebViewPackage called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Successfully got current WebView package");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to get current WebView package", e);
            }
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Returning null to use system default");
            return null;
        }
    }

    @ProxyMethod("getValidWebViewPackages")
    public static class GetValidWebViewPackages extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: getValidWebViewPackages called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null && (objInvoke instanceof PackageInfo[])) {
                    PackageInfo[] packageInfoArr = (PackageInfo[]) objInvoke;
                    if (packageInfoArr.length > 0) {
                        Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Found " + packageInfoArr.length + " valid WebView packages");
                        return objInvoke;
                    }
                }
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to get valid WebView packages", e);
            }
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Returning empty array to prevent conflicts");
            return new PackageInfo[0];
        }
    }

    @ProxyMethod("getWebViewPackages")
    public static class GetWebViewPackages extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: getWebViewPackages called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Successfully got WebView packages");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to get WebView packages", e);
            }
            return new ArrayList();
        }
    }

    @ProxyMethod("getWebViewProvider")
    public static class GetWebViewProvider extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: getWebViewProvider called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke == null) {
                    return null;
                }
                Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Successfully got WebView provider");
                return objInvoke;
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to get WebView provider", e);
                return null;
            }
        }
    }

    @ProxyMethod("getWebViewProviderInfo")
    public static class GetWebViewProviderInfo extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: getWebViewProviderInfo called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke == null) {
                    return null;
                }
                Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Successfully got WebView provider info");
                return objInvoke;
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to get WebView provider info", e);
                return null;
            }
        }
    }

    @ProxyMethod("isMultiProcessEnabled")
    public static class IsMultiProcessEnabled extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: isMultiProcessEnabled called, returning false for single-process mode");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isWebViewPackage")
    public static class IsWebViewPackage extends MethodHook {
        private boolean isKnownWebViewPackage(String str) {
            if (str == null) {
                return false;
            }
            return str.equals("com.google.android.webview") || str.equals("com.google.android.webview.dev") || str.equals("com.google.android.webview.beta") || str.equals("com.google.android.webview.canary") || str.equals("com.android.webview") || str.equals("com.huawei.webview") || str.equals("com.samsung.android.webview") || str.equals("com.oneplus.webview");
        }

        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                String str = (String) args[0];
                Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: isWebViewPackage called for: " + str);
                if (isKnownWebViewPackage(str)) {
                    Slog.d(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: " + str + " is a known WebView package");
                    return Boolean.TRUE;
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(IWebViewUpdateServiceProxy.TAG, "WebViewUpdateService: Failed to check WebView package", e);
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
