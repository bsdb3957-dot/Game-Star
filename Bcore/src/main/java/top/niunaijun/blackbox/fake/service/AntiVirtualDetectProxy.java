package top.niunaijun.blackbox.fake.service;

import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;

public class AntiVirtualDetectProxy implements IInjectHook {
    private static final String TAG = "AntiVirtualDetect";
    private static volatile boolean sInstalled;

    public static void install() {
        if (sInstalled) {
            return;
        }
        synchronized (AntiVirtualDetectProxy.class) {
            if (sInstalled) {
                return;
            }
            try {
                Runtime.class.getDeclaredMethod("exit", Integer.TYPE);
                sInstalled = true;
                Slog.d(TAG, "AntiVirtualDetect proxy installed");
            } catch (Throwable th) {
                Slog.w(TAG, "install failed: " + th.getMessage(), th);
            }
        }
    }

    public static boolean isAntiDetectProvider(String str) {
        if (str == null) {
            return false;
        }
        return str.contains("HadesContentProvider") || str.contains("hades") || str.contains("ztuni") || str.contains("SecurityGuard") || str.contains("AvDetector") || str.contains("VirtualDetect") || str.contains("SandBoxDetect") || str.contains("EmulatorDetect");
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
