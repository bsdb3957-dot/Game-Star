package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;

public class NetworkPermissionCompat implements IInjectHook {
    private static final String TAG = "NetworkPermissionCompat";
    private static volatile boolean sInstalled;

    public static void install() {
        if (sInstalled) {
            return;
        }
        synchronized (NetworkPermissionCompat.class) {
            if (sInstalled) {
                return;
            }
            try {
                Method declaredMethod = Class.forName("android.permission.PermissionManager").getDeclaredMethod("disablePermissionCache", null);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(null, null);
                sInstalled = true;
                Slog.d(TAG, "disabled framework permission cache");
            } catch (Throwable th) {
                Slog.w(TAG, "install failed: " + th.getMessage(), th);
            }
        }
    }

    @Override
    public void injectHook() {
        install();
    }

    @Override
    public boolean isBadEnv() {
        return !sInstalled;
    }
}
