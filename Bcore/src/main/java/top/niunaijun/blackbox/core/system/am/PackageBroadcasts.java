package top.niunaijun.blackbox.core.system.am;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.pm.BPackageManagerService;
import top.niunaijun.blackbox.core.system.pm.PackageMonitor;
import top.niunaijun.blackbox.core.system.user.BUserInfo;
import top.niunaijun.blackbox.core.system.user.BUserManagerService;
import top.niunaijun.blackbox.proxy.ProxyBroadcastReceiver;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Announces virtual package lifecycle broadcasts to guest apps: the host
 * framework never sees virtual installs, so these never arrive on their own.
 */
public class PackageBroadcasts implements PackageMonitor {
    private static final String TAG = "PackageBroadcasts";
    private static final long INSTALL_DELAY_MS = 5000;
    private static final long CATCH_UP_DELAY_MS = 10000;
    private static final PackageBroadcasts sInstance = new PackageBroadcasts();

    private final Handler mHandler;
    private final Set<String> mInstalled = new HashSet<>();
    private final Properties mState;
    private final File mStateFile;

    private PackageBroadcasts() {
        mState = new Properties();
        mStateFile = new File(BEnvironment.getSystemDir(), "package-broadcasts.conf");
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
        try (FileInputStream in = new FileInputStream(mStateFile)) {
            mState.load(in);
        } catch (Throwable ignored) {
        }
    }

    public static PackageBroadcasts get() {
        return sInstance;
    }

    private static String key(String packageName, int userId) {
        return userId + "/" + packageName;
    }

    private static String stateKey(int userId) {
        return "gms-caught-up/" + userId;
    }

    private void save() {
        try (FileOutputStream out = new FileOutputStream(mStateFile)) {
            mState.store(out, "users whose Play services has heard of the packages installed before it");
        } catch (Throwable t) {
            Slog.w(TAG, "could not save state: " + t);
        }
    }

    private static List<String> installedPackages(int userId) {
        List<String> packages = new ArrayList<>();
        try {
            List<PackageInfo> infos = BPackageManagerService.get().getInstalledPackages(0, userId);
            if (infos != null) {
                for (PackageInfo info : infos) {
                    packages.add(info.packageName);
                }
            }
        } catch (Throwable ignored) {
        }
        return packages;
    }

    private static boolean hasReceiver(Intent intent, int userId) {
        try {
            List<ResolveInfo> receivers = BPackageManagerService.get().queryBroadcastReceivers(intent, 128, null, userId);
            if (receivers != null) {
                for (ResolveInfo receiver : receivers) {
                    if (receiver.activityInfo != null) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void announce(String action, String packageName, boolean replacing, int userId, List<String> targets) {
        int appId = BPackageManagerService.get().getAppId(packageName);
        String hostPkg = BlackBoxCore.getHostPkg();
        for (String target : targets) {
            Intent intent = new Intent(action, Uri.fromParts("package", packageName, null));
            if (appId >= 0) {
                intent.putExtra("android.intent.extra.UID", appId);
            }
            if (replacing) {
                intent.putExtra("android.intent.extra.REPLACING", true);
            }
            if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !replacing) {
                intent.putExtra("android.intent.extra.DATA_REMOVED", true);
            }
            if (!ProxyBroadcastReceiver.reachesManifestReceivers(intent, target, userId)) {
                continue;
            }
            intent.setPackage(target);
            if (!hasReceiver(intent, userId)) {
                continue;
            }
            try {
                Intent stub = new Intent(ProxyManifest.getStubReceiverAction()).setPackage(hostPkg);
                ProxyBroadcastRecord.saveStub(stub, intent, userId);
                BlackBoxCore.getContext().sendBroadcast(stub);
                Slog.d(TAG, action + " " + packageName + " -> " + target + " (user " + userId + ")");
            } catch (Throwable t) {
                Slog.w(TAG, "could not send " + action + " for " + packageName + " to " + target + ": " + t);
            }
        }
    }

    private void announceInstalled(String packageName, int userId, boolean replacing) {
        if (!BPackageManagerService.get().isInstalled(packageName, userId)) {
            return;
        }
        List<String> targets = installedPackages(userId);
        targets.remove(packageName);
        if (!replacing) {
            announce("android.intent.action.PACKAGE_ADDED", packageName, false, userId, targets);
        } else {
            announce("android.intent.action.PACKAGE_REMOVED", packageName, true, userId, targets);
            announce("android.intent.action.PACKAGE_ADDED", packageName, true, userId, targets);
            announce("android.intent.action.PACKAGE_REPLACED", packageName, true, userId, targets);
        }
    }

    private void announceUninstalled(String packageName, int userId) {
        List<String> targets = installedPackages(userId);
        targets.remove(packageName);
        announce("android.intent.action.PACKAGE_REMOVED", packageName, false, userId, targets);
        announce("android.intent.action.PACKAGE_FULLY_REMOVED", packageName, false, userId, targets);
    }

    /** Tells an installed Play services about packages that predate it. */
    private void catchUpPlayServices(int userId) {
        synchronized (mState) {
            if (mState.getProperty(stateKey(userId)) != null) {
                return;
            }
        }
        if (!BPackageManagerService.get().isInstalled(GmsCore.GMS_PKG, userId)) {
            return;
        }
        int announced = 0;
        for (String packageName : installedPackages(userId)) {
            if (GmsCore.isGoogleAppOrService(packageName)) {
                continue;
            }
            announce("android.intent.action.PACKAGE_ADDED", packageName, false, userId,
                    Collections.singletonList(GmsCore.GMS_PKG));
            announced++;
        }
        synchronized (mState) {
            mState.setProperty(stateKey(userId), "1");
            save();
        }
        Slog.i(TAG, "told Play services in user " + userId + " about " + announced + " earlier package(s)");
    }

    @Override
    public void onPackageInstalled(String packageName, int userId) {
        if (userId < 0) {
            return;
        }
        boolean isNewInstall;
        synchronized (mInstalled) {
            isNewInstall = mInstalled.add(key(packageName, userId));
        }
        if (GmsCore.GMS_PKG.equals(packageName) && isNewInstall) {
            synchronized (mState) {
                mState.remove(stateKey(userId));
            }
            mHandler.postDelayed(() -> catchUpPlayServices(userId), CATCH_UP_DELAY_MS);
        }
        mHandler.postDelayed(() -> announceInstalled(packageName, userId, !isNewInstall), INSTALL_DELAY_MS);
    }

    @Override
    public void onPackageUninstalled(String packageName, boolean removeApp, int userId) {
        if (userId < 0) {
            return;
        }
        synchronized (mInstalled) {
            mInstalled.remove(key(packageName, userId));
        }
        if (GmsCore.GMS_PKG.equals(packageName)) {
            synchronized (mState) {
                if (mState.remove(stateKey(userId)) != null) {
                    save();
                }
            }
        }
        mHandler.post(() -> announceUninstalled(packageName, userId));
    }

    public void retellPlayServices(String packageName, int userId) {
        mHandler.post(() -> {
            if (BPackageManagerService.get().isInstalled(GmsCore.GMS_PKG, userId)
                    && BPackageManagerService.get().isInstalled(packageName, userId)) {
                announce("android.intent.action.PACKAGE_ADDED", packageName, false, userId,
                        Collections.singletonList(GmsCore.GMS_PKG));
            }
        });
    }

    public void systemReady() {
        for (BUserInfo user : BUserManagerService.get().getUsers()) {
            for (String packageName : installedPackages(user.id)) {
                synchronized (mInstalled) {
                    mInstalled.add(key(packageName, user.id));
                }
            }
        }
        BPackageManagerService.get().addPackageMonitor(this);
        for (BUserInfo user : BUserManagerService.get().getUsers()) {
            if (BPackageManagerService.get().isInstalled(GmsCore.GMS_PKG, user.id)) {
                mHandler.postDelayed(() -> catchUpPlayServices(user.id), CATCH_UP_DELAY_MS);
            }
        }
    }
}
