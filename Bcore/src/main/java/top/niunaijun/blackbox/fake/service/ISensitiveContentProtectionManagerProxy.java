package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.IBinder;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ISensitiveContentProtectionManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ISensitiveContentProtection";

    @ProxyMethod("setSensitiveContentProtection")
    public static class SetSensitiveContentProtection extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Object proxyInvocation = args[i];
                    if (proxyInvocation instanceof String) {
                        String str = (String) proxyInvocation;
                        if (!str.equals(BlackBoxCore.getHostPkg())) {
                            Slog.d(ISensitiveContentProtectionManagerProxy.TAG,
                                    "Fixing package name in setSensitiveContentProtection: " + str + " -> " + BlackBoxCore.getHostPkg());
                            args[i] = BlackBoxCore.getHostPkg();
                        }
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    public ISensitiveContentProtectionManagerProxy() {
        super(BRServiceManager.get().getService("sensitive_content_protection_service"));
    }

    @Override
    public Object getWho() {
        try {
            IBinder service = BRServiceManager.get().getService("sensitive_content_protection_service");
            if (service == null) {
                return null;
            }
            return Class.forName("android.view.ISensitiveContentProtectionManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, service);
        } catch (Exception e) {
            Slog.d(TAG, "getWho error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        if (BRServiceManager.get().getService("sensitive_content_protection_service") == null) {
            Slog.d(TAG, "Skipping SensitiveContentProtectionManagerService hook (service not found)");
        } else {
            replaceSystemService("sensitive_content_protection_service");
            Slog.d(TAG, "Hooked SensitiveContentProtectionManagerService");
        }
    }

    @Override
    public boolean isBadEnv() {
        IBinder service = BRServiceManager.get().getService("sensitive_content_protection_service");
        return (service == null || service == this) ? false : true;
    }
}
