package top.niunaijun.blackbox.core.system.am;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.pm.BPackageManagerService;
import top.niunaijun.blackbox.core.system.pm.PackageMonitor;
import top.niunaijun.blackbox.core.system.user.BUserInfo;
import top.niunaijun.blackbox.core.system.user.BUserManagerService;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Delivers virtual BOOT_COMPLETED broadcasts to guest apps, once per host
 * boot, tracked in virtual-boot.conf.
 */
public class VirtualBoot implements PackageMonitor {
    private static final String TAG = "VirtualBoot";
    private static final long INSTALL_DELAY_MS = 5000;
    private static final String[] BOOT_ACTIONS = {
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "android.intent.action.BOOT_COMPLETED"};
    private static final VirtualBoot sInstance = new VirtualBoot();

    private final Handler mHandler;
    private final Properties mState;
    private final File mStateFile;

    private VirtualBoot() {
        mState = new Properties();
        mStateFile = new File(BEnvironment.getSystemDir(), "virtual-boot.conf");
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
        try (FileInputStream in = new FileInputStream(mStateFile)) {
            mState.load(in);
        } catch (Throwable ignored) {
        }
    }

    public static VirtualBoot get() {
        return sInstance;
    }

    private static String key(String packageName, int userId) {
        return userId + "/" + packageName;
    }

    private static String currentBootId() {
        try {
            return "c" + Settings.Global.getInt(BlackBoxCore.getContext().getContentResolver(), "boot_count");
        } catch (Throwable ignored) {
            // elapsed time since epoch in minutes, stable within one boot
            return "t" + ((System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 60000);
        }
    }

    private void save() {
        try (FileOutputStream out = new FileOutputStream(mStateFile)) {
            mState.store(out, "packages that have seen this host boot");
        } catch (Throwable t) {
            Slog.w(TAG, "could not save boot state: " + t);
        }
    }

    private void bootIfNeeded(String packageName, int userId) {
        String bootId = currentBootId();
        synchronized (mState) {
            if (bootId.equals(mState.getProperty(key(packageName, userId)))) {
                return;
            }
        }
        if (!BPackageManagerService.get().isInstalled(packageName, userId)) {
            return;
        }
        boolean booted = false;
        for (String action : BOOT_ACTIONS) {
            Intent intent = new Intent(action).setPackage(packageName);
            try {
                List<ResolveInfo> receivers = BPackageManagerService.get().queryBroadcastReceivers(intent, 128, null, userId);
                if (receivers == null || receivers.isEmpty()) {
                    continue;
                }
                String hostPkg = BlackBoxCore.getHostPkg();
                Intent stub = new Intent(ProxyManifest.getStubReceiverAction()).setPackage(hostPkg);
                ProxyBroadcastRecord.saveStub(stub, intent, userId);
                BlackBoxCore.getContext().sendBroadcast(stub);
                booted = true;
            } catch (Throwable ignored) {
            }
        }
        synchronized (mState) {
            mState.setProperty(key(packageName, userId), bootId);
            save();
        }
        if (booted) {
            Slog.i(TAG, "booted " + packageName + " in user " + userId + " (" + bootId + ")");
        }
    }

    @Override
    public void onPackageInstalled(String packageName, int userId) {
        mHandler.postDelayed(() -> bootIfNeeded(packageName, userId), INSTALL_DELAY_MS);
    }

    @Override
    public void onPackageUninstalled(String packageName, boolean removeApp, int userId) {
        synchronized (mState) {
            if (mState.remove(key(packageName, userId)) != null) {
                save();
            }
        }
    }

    public void systemReady() {
        BPackageManagerService.get().addPackageMonitor(this);
        mHandler.post(() -> {
            for (BUserInfo user : BUserManagerService.get().getUsers()) {
                List<PackageInfo> packages = BPackageManagerService.get().getInstalledPackages(0, user.id);
                if (packages == null) {
                    continue;
                }
                for (PackageInfo info : packages) {
                    bootIfNeeded(info.packageName, user.id);
                }
            }
        });
    }
}
