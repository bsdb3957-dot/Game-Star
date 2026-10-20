package top.niunaijun.blackbox.proxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.PowerManager;
import android.os.RemoteException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.am.PendingResultData;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;

/**
 * Created by BlackBox on 2022/2/25.
 */
public class ProxyBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "ProxyBroadcastReceiver";

    // opaque mask from the host framework: registered-only + replace-pending
    private static final int FLAG_RECEIVER_FRAMEWORK_STATE = 1082130432;
    private static final int FLAG_RECEIVER_INCLUDE_BACKGROUND = 0x1000000;

    // implicit broadcasts exempt from the background delivery check
    private static final Set<String> IMPLICIT_EXEMPT = new HashSet<>(Arrays.asList(
            "android.intent.action.LOCKED_BOOT_COMPLETED", "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.USER_INITIALIZE", "android.intent.action.USER_ADDED",
            "android.intent.action.USER_REMOVED", "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED", "android.app.action.NEXT_ALARM_CLOCK_CHANGED",
            "android.intent.action.LOCALE_CHANGED", "android.accounts.LOGIN_ACCOUNTS_CHANGED",
            "android.accounts.action.ACCOUNT_REMOVED", "android.intent.action.PACKAGE_DATA_CLEARED",
            "android.intent.action.PACKAGE_FULLY_REMOVED", "android.intent.action.NEW_OUTGOING_CALL",
            "android.intent.action.MEDIA_MOUNTED", "android.intent.action.MEDIA_EJECT",
            "android.intent.action.MEDIA_UNMOUNTED", "android.intent.action.MEDIA_REMOVED",
            "android.provider.Telephony.SMS_RECEIVED", "android.provider.Telephony.WAP_PUSH_RECEIVED",
            "android.intent.action.PHONE_STATE", "android.intent.action.EVENT_REMINDER"));

    /**
     * Manifest-declared receivers only fire for implicit broadcasts when the
     * target is not background-restricted; mirrors the framework's own gate.
     */
    public static boolean reachesManifestReceivers(Intent intent, String packageName, int userId) {
        if ((intent.getFlags() & FLAG_RECEIVER_FRAMEWORK_STATE) == 0) {
            if ((intent.getFlags() & FLAG_RECEIVER_INCLUDE_BACKGROUND) != 0
                    || IMPLICIT_EXEMPT.contains(intent.getAction())) {
                return true;
            }
            try {
                ApplicationInfo applicationInfo = BlackBoxCore.getBPackageManager().getApplicationInfo(packageName, 0, userId);
                if (applicationInfo != null && applicationInfo.targetSdkVersion < 26) {
                    return true;
                }
                PowerManager powerManager = (PowerManager) BlackBoxCore.getContext().getSystemService(Context.POWER_SERVICE);
                if (powerManager != null && powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    return true;
                }
            } catch (Throwable t) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setExtrasClassLoader(context.getClassLoader());
        ProxyBroadcastRecord record = ProxyBroadcastRecord.create(intent);
        if (record.mIntent == null) {
            return;
        }
        PendingResult pendingResult = goAsync();
        try {
            BlackBoxCore.getBActivityManager().scheduleBroadcastReceiver(record.mIntent, new PendingResultData(pendingResult), record.mUserId);
        } catch (RemoteException e) {
            pendingResult.finish();
        }
    }
}