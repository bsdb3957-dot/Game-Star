package top.niunaijun.blackbox.fake.hook;

import android.util.Log;

import android.os.Build;

import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.delegate.AppInstrumentation;
import top.niunaijun.blackbox.fake.service.AndroidIdProxy;
import top.niunaijun.blackbox.fake.service.ApkAssetsProxy;
import top.niunaijun.blackbox.fake.service.AudioPermissionProxy;
import top.niunaijun.blackbox.fake.service.AudioRecordProxy;
import top.niunaijun.blackbox.fake.service.AuthenticationProxy;
import top.niunaijun.blackbox.fake.service.ClassLoaderProxy;
import top.niunaijun.blackbox.fake.service.ContentResolverProxy;
import top.niunaijun.blackbox.fake.service.DeviceIdProxy;
import top.niunaijun.blackbox.fake.service.FeatureFlagUtilsProxy;
import top.niunaijun.blackbox.fake.service.FileSystemProxy;
import top.niunaijun.blackbox.fake.service.GmsProxy;
import top.niunaijun.blackbox.fake.service.HCallbackProxy;
import top.niunaijun.blackbox.fake.service.IAccessibilityManagerProxy;
import top.niunaijun.blackbox.fake.service.IAccountManagerProxy;
import top.niunaijun.blackbox.fake.service.IActivityClientProxy;
import top.niunaijun.blackbox.fake.service.IActivityManagerProxy;
import top.niunaijun.blackbox.fake.service.IActivityTaskManagerProxy;
import top.niunaijun.blackbox.fake.service.IAlarmManagerProxy;
import top.niunaijun.blackbox.fake.service.IAppOpsManagerProxy;
import top.niunaijun.blackbox.fake.service.IAppWidgetManagerProxy;
import top.niunaijun.blackbox.fake.service.IAttributionSourceProxy;
import top.niunaijun.blackbox.fake.service.IAudioServiceProxy;
import top.niunaijun.blackbox.fake.service.IAutofillManagerProxy;
import top.niunaijun.blackbox.fake.service.IBackupManagerProxy;
import top.niunaijun.blackbox.fake.service.IClipboardProxy;
import top.niunaijun.blackbox.fake.service.IConnectivityManagerProxy;
import top.niunaijun.blackbox.fake.service.IContextHubServiceProxy;
import top.niunaijun.blackbox.fake.service.IContentProviderProxy;
import top.niunaijun.blackbox.fake.service.ICrossProfileAppsProxy;
import top.niunaijun.blackbox.fake.service.IDeviceIdentifiersPolicyProxy;
import top.niunaijun.blackbox.fake.service.IDevicePolicyManagerProxy;
import top.niunaijun.blackbox.fake.service.IDisplayManagerProxy;
import top.niunaijun.blackbox.fake.service.IDnsResolverProxy;
import top.niunaijun.blackbox.fake.service.IFingerprintManagerProxy;
import top.niunaijun.blackbox.fake.service.IGraphicsStatsProxy;
import top.niunaijun.blackbox.fake.service.IJobServiceProxy;
import top.niunaijun.blackbox.fake.service.ILauncherAppsProxy;
import top.niunaijun.blackbox.fake.service.ILocaleManagerProxy;
import top.niunaijun.blackbox.fake.service.ILocationManagerProxy;
import top.niunaijun.blackbox.fake.service.IMediaRouterServiceProxy;
import top.niunaijun.blackbox.fake.service.IMediaSessionManagerProxy;
import top.niunaijun.blackbox.fake.service.IMiuiSecurityManagerProxy;
import top.niunaijun.blackbox.fake.service.INetworkManagementServiceProxy;
import top.niunaijun.blackbox.fake.service.INotificationManagerProxy;
import top.niunaijun.blackbox.fake.service.IPackageManagerProxy;
import top.niunaijun.blackbox.fake.service.IPermissionManagerProxy;
import top.niunaijun.blackbox.fake.service.IPersistentDataBlockServiceProxy;
import top.niunaijun.blackbox.fake.service.IPhoneSubInfoProxy;
import top.niunaijun.blackbox.fake.service.IPowerManagerProxy;
import top.niunaijun.blackbox.fake.service.ISafetyCenterManagerProxy;
import top.niunaijun.blackbox.fake.service.ISensitiveContentProtectionManagerProxy;
import top.niunaijun.blackbox.fake.service.ISensorPrivacyManagerProxy;
import top.niunaijun.blackbox.fake.service.ISettingsProviderProxy;
import top.niunaijun.blackbox.fake.service.ISettingsSystemProxy;
import top.niunaijun.blackbox.fake.service.IShortcutManagerProxy;
import top.niunaijun.blackbox.fake.service.IStorageManagerProxy;
import top.niunaijun.blackbox.fake.service.IStorageStatsManagerProxy;
import top.niunaijun.blackbox.fake.service.ISubProxy;
import top.niunaijun.blackbox.fake.service.ISystemSensorManagerProxy;
import top.niunaijun.blackbox.fake.service.ISystemUpdateProxy;
import top.niunaijun.blackbox.fake.service.ITelephonyManagerProxy;
import top.niunaijun.blackbox.fake.service.ITelephonyRegistryProxy;
import top.niunaijun.blackbox.fake.service.ITetheringConnectorProxy;
import top.niunaijun.blackbox.fake.service.IUserManagerProxy;
import top.niunaijun.blackbox.fake.service.IVibratorServiceProxy;
import top.niunaijun.blackbox.fake.service.IVpnManagerProxy;
import top.niunaijun.blackbox.fake.service.IWifiManagerProxy;
import top.niunaijun.blackbox.fake.service.IWifiScannerProxy;
import top.niunaijun.blackbox.fake.service.IWindowManagerProxy;
import top.niunaijun.blackbox.fake.service.IXiaomiAttributionSourceProxy;
import top.niunaijun.blackbox.fake.service.IXiaomiMiuiServicesProxy;
import top.niunaijun.blackbox.fake.service.IXiaomiSettingsProxy;
import top.niunaijun.blackbox.fake.service.LevelDbProxy;
import top.niunaijun.blackbox.fake.service.MediaRecorderClassProxy;
import top.niunaijun.blackbox.fake.service.MediaRecorderProxy;
import top.niunaijun.blackbox.fake.service.NetworkPermissionCompat;
import top.niunaijun.blackbox.fake.service.ReLinkerProxy;
import top.niunaijun.blackbox.fake.service.ResourcesManagerProxy;
import top.niunaijun.blackbox.fake.service.SQLiteDatabaseProxy;
import top.niunaijun.blackbox.fake.service.StatsManagerStub;
import top.niunaijun.blackbox.fake.service.SystemLibraryProxy;
import top.niunaijun.blackbox.fake.service.SystemServiceGuard;
import top.niunaijun.blackbox.fake.service.WorkManagerProxy;
import top.niunaijun.blackbox.fake.service.context.ContentServiceStub;
import top.niunaijun.blackbox.fake.service.context.RestrictionsManagerStub;
import top.niunaijun.blackbox.fake.service.libcore.OsStub;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

/**
 * Created by Milk on 3/30/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class HookManager {
    public static final String TAG = "HookManager";

    private static final HookManager sHookManager = new HookManager();

    private final Map<Class<?>, IInjectHook> mInjectors = new HashMap<>();

    public static HookManager get() {
        return sHookManager;
    }

    private void handleHookError(IInjectHook injectHook, Exception e) {
        String simpleName = injectHook.getClass().getSimpleName();
        Slog.e(TAG, "Hook failed: " + simpleName + " - " + e.getMessage(), e);
        if (simpleName.contains("ActivityManager") || simpleName.contains("PackageManager")
                || simpleName.contains("WebView") || simpleName.contains("ContentProvider")) {
            Slog.w(TAG, "Critical hook failed: " + simpleName + ", attempting recovery");
            try {
                if (injectHook.isBadEnv()) {
                    Slog.d(TAG, "Attempting to recover hook: " + simpleName);
                    injectHook.injectHook();
                }
            } catch (Exception recoveryError) {
                Slog.e(TAG, "Hook recovery failed: " + simpleName, recoveryError);
            }
        }
    }

    public boolean areCriticalHooksInstalled() {
        String[] critical = {"IActivityManagerProxy", "IPackageManagerProxy", IContentProviderProxy.TAG};
        for (String name : critical) {
            boolean found = false;
            for (Class<?> clazz : mInjectors.keySet()) {
                if (clazz.getSimpleName().equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Slog.w(TAG, "Critical hook missing: " + name);
                return false;
            }
        }
        Slog.d(TAG, "All critical hooks are installed");
        return true;
    }

    public void init() {
        if (BlackBoxCore.get().isBlackProcess() || BlackBoxCore.get().isServerProcess()) {
            addInjector(new IDisplayManagerProxy());
            addInjector(new OsStub());
            addInjector(new IActivityManagerProxy());
            addInjector(new IPackageManagerProxy());
            addInjector(new ITelephonyManagerProxy());
            addInjector(new HCallbackProxy());
            addInjector(new IAppOpsManagerProxy());
            addInjector(new INotificationManagerProxy());
            addInjector(new IAlarmManagerProxy());
            addInjector(new IAppWidgetManagerProxy());
            addInjector(new ContentServiceStub());
            addInjector(new IWindowManagerProxy());
            addInjector(new IUserManagerProxy());
            addInjector(new RestrictionsManagerStub());
            addInjector(new IMediaSessionManagerProxy());
            addInjector(new IAudioServiceProxy());
            addInjector(new ISensorPrivacyManagerProxy());
            addInjector(new ContentResolverProxy());
            addInjector(new SystemLibraryProxy());
            addInjector(new ReLinkerProxy());
            addInjector(new WorkManagerProxy());
            addInjector(new MediaRecorderProxy());
            addInjector(new AudioRecordProxy());
            addInjector(new IMiuiSecurityManagerProxy());
            addInjector(new ISettingsProviderProxy());
            addInjector(new FeatureFlagUtilsProxy());
            addInjector(new MediaRecorderClassProxy());
            addInjector(new SQLiteDatabaseProxy());
            addInjector(new ClassLoaderProxy());
            addInjector(new FileSystemProxy());
            addInjector(new GmsProxy());
            addInjector(new LevelDbProxy());
            addInjector(new DeviceIdProxy());
            addInjector(new AuthenticationProxy());
            addInjector(new AndroidIdProxy());
            addInjector(new AudioPermissionProxy());
            addInjector(new ILocationManagerProxy());
            addInjector(new IStorageManagerProxy());
            addInjector(new ILauncherAppsProxy());
            addInjector(new IJobServiceProxy());
            addInjector(new IAccessibilityManagerProxy());
            addInjector(new ITelephonyRegistryProxy());
            addInjector(new IDevicePolicyManagerProxy());
            addInjector(new IBackupManagerProxy());
            addInjector(new ISafetyCenterManagerProxy());
            addInjector(new StatsManagerStub());
            if (Build.VERSION.SDK_INT >= 33) {
                addInjector(new ILocaleManagerProxy());
            }
            addInjector(new IClipboardProxy());
            addInjector(new ICrossProfileAppsProxy());
            addInjector(new ITetheringConnectorProxy());
            addInjector(new IAccountManagerProxy());
            addInjector(new NetworkPermissionCompat());
            addInjector(new IConnectivityManagerProxy());
            addInjector(new IDnsResolverProxy());
            addInjector(new IAttributionSourceProxy());
            addInjector(new IContentProviderProxy());
            addInjector(new ISettingsSystemProxy());
            addInjector(new ISystemSensorManagerProxy());
            addInjector(new IXiaomiAttributionSourceProxy());
            addInjector(new IXiaomiSettingsProxy());
            addInjector(new IXiaomiMiuiServicesProxy());
            addInjector(new IPhoneSubInfoProxy());
            addInjector(new ISubProxy());
            addInjector(new IMediaRouterServiceProxy());
            addInjector(new IPowerManagerProxy());
            addInjector(new IContextHubServiceProxy());
            addInjector(new IVibratorServiceProxy());
            addInjector(new IPersistentDataBlockServiceProxy());
            addInjector(AppInstrumentation.get());
            /*
            * It takes time to test and enhance the compatibility of WifiManager
            * (only tested in Android 10).
            * commented by BlackBoxing at 2022/03/08
            * */
            addInjector(new IWifiManagerProxy());
            addInjector(new IWifiScannerProxy());
            addInjector(new ApkAssetsProxy());
            addInjector(new ResourcesManagerProxy());
            // 12.0
            if (BuildCompat.isS()) {
                addInjector(new IActivityClientProxy(null));
                addInjector(new IVpnManagerProxy());
            }
            if (BuildCompat.isS()) {
                addInjector(new ISensitiveContentProtectionManagerProxy());
            }
            // 11.0
            if (BuildCompat.isR()) {
                addInjector(new IPermissionManagerProxy());
            }
            // 10.0
            if (BuildCompat.isQ()) {
                addInjector(new IActivityTaskManagerProxy());
            }
            // 9.0
            if (BuildCompat.isPie()) {
                addInjector(new ISystemUpdateProxy());
            }
            // 8.0
            if (BuildCompat.isOreo()) {
                addInjector(new IAutofillManagerProxy());
                addInjector(new IDeviceIdentifiersPolicyProxy());
                addInjector(new IStorageStatsManagerProxy());
            }
            // 7.1
            if (BuildCompat.isN_MR1()) {
                addInjector(new IShortcutManagerProxy());
            }
            // 7.0
            if (BuildCompat.isN()) {
                addInjector(new INetworkManagementServiceProxy());
            }
            // 6.0
            if (BuildCompat.isM()) {
                addInjector(new IFingerprintManagerProxy());
                addInjector(new IGraphicsStatsProxy());
            }
            // 5.0
            if (BuildCompat.isL()) {
                addInjector(new IJobServiceProxy());
            }
        }
        injectAll();
        try {
            new SystemServiceGuard().injectHook();
        } catch (Throwable e) {
            Slog.w(TAG, "SystemServiceGuard failed: " + e);
        }
    }

    public void checkEnv(Class<?> clazz) {
        IInjectHook iInjectHook = mInjectors.get(clazz);
        if (iInjectHook != null && iInjectHook.isBadEnv()) {
            Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env");
            iInjectHook.injectHook();
        }
    }

    public void checkAll() {
        for (Class<?> aClass : mInjectors.keySet()) {
            IInjectHook iInjectHook = mInjectors.get(aClass);
            if (iInjectHook != null && iInjectHook.isBadEnv()) {
                Log.d(TAG, "checkEnv: " + aClass.getSimpleName() + " is bad env");
                iInjectHook.injectHook();
            }
        }
    }

    void addInjector(IInjectHook injectHook) {
        mInjectors.put(injectHook.getClass(), injectHook);
    }

    void injectAll() {
        for (IInjectHook value : mInjectors.values()) {
            try {
                Slog.d(TAG, "hook: " + value);
                value.injectHook();
            } catch (Exception e) {
                Slog.d(TAG, "hook error: " + value);
                handleHookError(value, e);
            }
        }
    }

    public void reinitializeHooks() {
        Slog.d(TAG, "Reinitializing all hooks");
        mInjectors.clear();
        init();
        Slog.d(TAG, "Hook reinitialization completed");
    }
}
