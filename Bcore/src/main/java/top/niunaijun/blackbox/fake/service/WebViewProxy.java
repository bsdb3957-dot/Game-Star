package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.io.File;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class WebViewProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewProxy";

    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        private void configureWebView(WebView webView) {
            try {
                WebSettings settings = webView.getSettings();
                if (settings != null) {
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);
                    settings.setDatabaseEnabled(true);
                    settings.setCacheMode(-1);
                    try {
                        settings.getClass().getMethod("setAppCacheEnabled", Boolean.TYPE).invoke(settings, Boolean.TRUE);
                        if (webView.getContext() != null) {
                            settings.getClass().getMethod("setAppCachePath", String.class).invoke(settings, webView.getContext().getCacheDir().getAbsolutePath());
                        }
                    } catch (Throwable th) {
                        Slog.w(WebViewProxy.TAG, "WebView: AppCache not supported: " + th.getMessage());
                    }
                    settings.setBlockNetworkLoads(false);
                    settings.setBlockNetworkImage(false);
                    settings.setAllowFileAccess(true);
                    settings.setAllowFileAccessFromFileURLs(true);
                    settings.setAllowUniversalAccessFromFileURLs(true);
                    settings.setMixedContentMode(0);
                    String userAgentString = settings.getUserAgentString();
                    if (userAgentString != null && !userAgentString.contains("BlackBox")) {
                        settings.setUserAgentString(userAgentString.concat(" BlackBox"));
                    }
                    try {
                        webView.setNetworkAvailable(true);
                    } catch (Exception unused) {
                    }
                    settings.setAllowContentAccess(true);
                    if (Build.VERSION.SDK_INT >= 26) {
                        settings.setSafeBrowsingEnabled(false);
                    }
                    Slog.d(WebViewProxy.TAG, "WebView: Configured successfully with network access enabled");
                }
            } catch (Exception e) {
                Slog.w(WebViewProxy.TAG, "WebView: Failed to configure settings", e);
            }
        }

        private WebView createFallbackWebView(Context context) {
            if (context == null) {
                return null;
            }
            try {
                WebView webView = new WebView(context);
                WebSettings settings = webView.getSettings();
                if (settings != null) {
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);
                }
                Slog.d(WebViewProxy.TAG, "WebView: Created fallback WebView");
                return webView;
            } catch (Exception e) {
                Slog.e(WebViewProxy.TAG, "WebView: Failed to create fallback WebView", e);
                return null;
            }
        }

        /* JADX WARN: Code duplicated, block: B:11:0x001f A[Catch: Exception -> 0x001c, TryCatch #0 {Exception -> 0x001c, blocks: (B:4:0x000e, B:6:0x0011, B:8:0x0018, B:13:0x0025, B:15:0x0061, B:16:0x0073, B:17:0x00b3, B:19:0x00bb, B:11:0x001f), top: B:23:0x000e }] */
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewProxy.TAG, "WebView: Constructor called, intercepting to prevent data directory conflicts");
            Context context = null;
            if (args != null) {
                try {
                    if (args.length > 0) {
                        Object proxyInvocation = args[0];
                        if (proxyInvocation instanceof Context) {
                            context = (Context) proxyInvocation;
                        } else {
                            context = BlackBoxCore.getContext();
                        }
                    } else {
                        context = BlackBoxCore.getContext();
                    }
                } catch (Exception e) {
                    Slog.w(WebViewProxy.TAG, "WebView: Constructor failed, attempting fallback", e);
                    return createFallbackWebView(context);
                }
            } else {
                context = BlackBoxCore.getContext();
            }
            if (context != null) {
                context.getPackageName();
                String str = context.getApplicationInfo().dataDir + "/webview_" + String.valueOf(BActivityThread.getUserId()) + "_" + Process.myPid();
                File file = new File(str);
                if (!file.exists()) {
                    file.mkdirs();
                    Slog.d(WebViewProxy.TAG, "WebView: Created unique data directory: " + str);
                }
                System.setProperty("webview.data.dir", str);
                System.setProperty("webview.cache.dir", str + "/cache");
                System.setProperty("webview.cookies.dir", str + "/cookies");
                StringBuilder sb = new StringBuilder("WebView: Set custom data directory: ");
                sb.append(str);
                Slog.d(WebViewProxy.TAG, sb.toString());
            }
            Object objInvoke = method.invoke(who, args);
            if (objInvoke instanceof WebView) {
                configureWebView((WebView) objInvoke);
            }
            return objInvoke;
        }
    }

    @ProxyMethod("getDataDirectory")
    public static class GetDataDirectory extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                Slog.d(WebViewProxy.TAG, "WebView: getDataDirectory called, returning unique directory");
                Context context = BlackBoxCore.getContext();
                if (context == null) {
                    return method.invoke(who, args);
                }
                context.getPackageName();
                String str = context.getApplicationInfo().dataDir + "/webview_" + String.valueOf(BActivityThread.getUserId()) + "_" + Process.myPid();
                File file = new File(str);
                if (!file.exists()) {
                    file.mkdirs();
                }
                Slog.d(WebViewProxy.TAG, "WebView: Returning unique data directory: " + str);
                return str;
            } catch (Exception e) {
                Slog.w(WebViewProxy.TAG, "WebView: getDataDirectory failed, returning fallback", e);
                return "/data/data/" + BlackBoxCore.getHostPkg() + "/webview_fallback";
            }
        }
    }

    @ProxyMethod("getInstance")
    public static class GetWebViewDatabaseInstance extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(WebViewProxy.TAG, "WebView: getInstance called for WebViewDatabase");
            try {
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    context.getPackageName();
                    String str = context.getApplicationInfo().dataDir + "/webview_db_" + String.valueOf(BActivityThread.getUserId()) + "_" + Process.myPid();
                    System.setProperty("webview.database.path", str);
                    Slog.d(WebViewProxy.TAG, "WebView: Set unique database path: " + str);
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(WebViewProxy.TAG, "WebView: Failed to get WebViewDatabase instance", e);
                return null;
            }
        }
    }

    @ProxyMethod("loadUrl")
    public static class LoadUrl extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                String str = (String) args[0];
                Slog.d(WebViewProxy.TAG, "WebView: loadUrl called with: " + str);
                if (str != null && str.startsWith("file://")) {
                    Slog.d(WebViewProxy.TAG, "WebView: Handling file URL: " + (str));
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setDataDirectorySuffix")
    public static class SetDataDirectorySuffix extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null) {
                try {
                    if (args.length > 0) {
                        String str = (String) args[0];
                        Slog.d(WebViewProxy.TAG, "WebView: setDataDirectorySuffix called with: " + str);
                        Context context = BlackBoxCore.getContext();
                        if (context != null) {
                            context.getPackageName();
                        }
                        String str2 = str + "_" + String.valueOf(BActivityThread.getUserId()) + "_" + Process.myPid();
                        args[0] = str2;
                        Slog.d(WebViewProxy.TAG, "WebView: Using unique suffix: " + str2);
                    }
                } catch (Exception e) {
                    Slog.w(WebViewProxy.TAG, "WebView: setDataDirectorySuffix failed, continuing without suffix", e);
                    return null;
                }
            }
            return method.invoke(who, args);
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
