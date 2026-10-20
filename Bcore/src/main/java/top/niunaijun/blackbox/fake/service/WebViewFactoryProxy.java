package top.niunaijun.blackbox.fake.service;

import android.webkit.WebView;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class WebViewFactoryProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewFactoryProxy";

    @ProxyMethod("getWebViewPackage")
    public static class GetWebViewPackage extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewPackage called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView package");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView package", e);
            }
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning default WebView package: com.google.android.webview");
            return "com.google.android.webview";
        }
    }

    @ProxyMethod("getWebViewProviderClass")
    public static class GetWebViewProviderClass extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewProviderClass called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView provider class");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView provider class", e);
            }
            try {
                Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning default WebView class: " + (WebView.class.getName()));
                return WebView.class;
            } catch (Exception e) {
                Slog.e(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get default WebView class", e);
                return null;
            }
        }
    }

    @ProxyMethod("getWebViewProviderClassLoader")
    public static class GetWebViewProviderClassLoader extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewProviderClassLoader called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView provider class loader");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView provider class loader", e);
            }
            try {
                ClassLoader classLoader = WebView.class.getClassLoader();
                Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning current class loader: " + classLoader);
                return classLoader;
            } catch (Exception e) {
                Slog.e(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get current class loader", e);
                return null;
            }
        }
    }

    @ProxyMethod("getWebViewProviderClassLoader")
    public static class GetWebViewProviderClassLoaderWithPackage extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewProviderClassLoader called for package: " + ((String) args[0]));
            }
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView provider class loader");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView provider class loader", e);
            }
            try {
                ClassLoader classLoader = WebView.class.getClassLoader();
                Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning current class loader: " + classLoader);
                return classLoader;
            } catch (Exception e) {
                Slog.e(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get current class loader", e);
                return null;
            }
        }
    }

    @ProxyMethod("getWebViewProviderClass")
    public static class GetWebViewProviderClassWithPackage extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewProviderClass called for package: " + ((String) args[0]));
            }
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView provider class");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView provider class", e);
            }
            try {
                Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning default WebView class: " + (WebView.class.getName()));
                return WebView.class;
            } catch (Exception e) {
                Slog.e(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get default WebView class", e);
                return null;
            }
        }
    }

    @ProxyMethod("getWebViewProviderInfo")
    public static class GetWebViewProviderInfo extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewProviderInfo called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView provider info");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView provider info", e);
            }
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning null to use system default");
            return null;
        }
    }

    @ProxyMethod("getWebViewProviderPackage")
    public static class GetWebViewProviderPackage extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewProviderPackage called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView provider package");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView provider package", e);
            }
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning default WebView package: com.google.android.webview");
            return "com.google.android.webview";
        }
    }

    @ProxyMethod("getWebViewProviderPackageInfo")
    public static class GetWebViewProviderPackageInfo extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: getWebViewProviderPackageInfo called");
            try {
                Object objInvoke = method.invoke(who, args);
                if (objInvoke != null) {
                    Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Successfully got WebView provider package info");
                    return objInvoke;
                }
            } catch (Exception e) {
                Slog.w(WebViewFactoryProxy.TAG, "WebViewFactory: Failed to get WebView provider package info", e);
            }
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: Returning null to use system default");
            return null;
        }
    }

    @ProxyMethod("isWebViewSupported")
    public static class IsWebViewSupported extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewFactoryProxy.TAG, "WebViewFactory: isWebViewSupported called, returning true");
            return Boolean.TRUE;
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
