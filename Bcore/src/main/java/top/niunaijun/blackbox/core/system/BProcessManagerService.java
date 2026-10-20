package top.niunaijun.blackbox.core.system;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.IBActivityThread;
import top.niunaijun.blackbox.core.NativeCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.notification.BNotificationManagerService;
import top.niunaijun.blackbox.core.system.pm.BPackageManagerService;
import top.niunaijun.blackbox.core.system.user.BUserHandle;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ApplicationThreadCompat;
import top.niunaijun.blackbox.utils.compat.BundleCompat;
import top.niunaijun.blackbox.utils.provider.ProviderCall;

/**
 * Created by Milk on 4/2/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class BProcessManagerService implements ISystemService {
    public static final String TAG = "BProcessManager";
    private static final int START_ATTEMPTS = 3;
    private static final long START_WAIT_MS = 20000;

    public static BProcessManagerService sBProcessManagerService = new BProcessManagerService();
    private final Map<Integer, Map<String, ProcessRecord>> mProcessMap = new HashMap<>();
    private final List<ProcessRecord> mPidsSelfLocked = new ArrayList<>();
    private final Object mProcessLock = new Object();

    public static BProcessManagerService get() {
        return sBProcessManagerService;
    }

    public ProcessRecord startProcessLocked(String packageName, String processName, int userId, int bpid, int callingPid) {
        ApplicationInfo info = null;
        for (int i = 0; i < START_ATTEMPTS && info == null; i++) {
            info = BPackageManagerService.get().getApplicationInfo(packageName, i, userId);
        }
        if (info == null)
            return null;
        int buid = BUserHandle.getUid(userId, BPackageManagerService.get().getAppId(packageName));

        for (int attempt = 0; attempt < START_ATTEMPTS; attempt++) {
            ProcessRecord app;
            synchronized (mProcessLock) {
                Map<String, ProcessRecord> bProcess = mProcessMap.get(buid);
                if (bProcess == null) {
                    bProcess = new HashMap<>();
                }
                app = bProcess.get(processName);
                if (app != null && bpid != -1 && app.bpid != bpid && isAttached(app)) {
                    // the restarted stub pid takes over the process name
                    Slog.w(TAG, "restarted stub p" + bpid + " takes " + processName
                            + " while p" + app.bpid + " still runs it");
                    app = null;
                }
                if (app != null) {
                    if (app.starting) {
                        // another thread is starting it, wait for the attach
                    } else if (isAttached(app)) {
                        return app;
                    } else {
                        forgetLocked(app);
                    }
                    app.initLock.block(START_WAIT_MS);
                    if (isAttached(app)) {
                        return app;
                    }
                    forgetLocked(app);
                    app = null;
                }
                if (app == null) {
                    int usingBpid = bpid == -1 ? getUsingBPidL() : bpid;
                    if (usingBpid == -1) {
                        throw new RuntimeException("No processes available");
                    }
                    Slog.d(TAG, "init bUid = " + buid + ", bPid = " + usingBpid);
                    app = new ProcessRecord(info, processName);
                    app.uid = Process.myUid();
                    app.bpid = usingBpid;
                    app.buid = BPackageManagerService.get().getAppId(packageName);
                    app.callingBUid = getBUidByPidOrPackageName(callingPid, packageName);
                    app.userId = userId;
                    app.starting = true;
                    synchronized (mProcessMap) {
                        bProcess.put(processName, app);
                        mProcessMap.put(buid, bProcess);
                    }
                    synchronized (mPidsSelfLocked) {
                        mPidsSelfLocked.add(app);
                    }

                    boolean started;
                    try {
                        started = initAppProcessL(app);
                    } catch (Throwable t) {
                        Slog.w(TAG, "start of " + processName + " failed: " + t);
                        started = false;
                    }

                    synchronized (mProcessLock) {
                        app.starting = false;
                        if (started) {
                            int pid = getPid(BlackBoxCore.getContext(), ProxyManifest.getProcessName(app.bpid));
                            app.pid = pid;
                            if (!isAttached(app)) {
                                forgetLocked(app);
                                started = false;
                            } else if (!isMappedLocked(app)) {
                                // the record was replaced while starting
                                app.kill();
                                forgetLocked(app);
                                started = false;
                            }
                        } else {
                            forgetLocked(app);
                        }
                    }
                    app.initLock.open();
                    if (started) {
                        return app;
                    }
                }
            }
            SystemClock.sleep(300);
        }
        return null;
    }

    private int getUsingBPidL() {
        ActivityManager manager = (ActivityManager) BlackBoxCore.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
        Set<Integer> usingPs = new HashSet<>();
        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            int i = parseBPid(runningAppProcess.processName);
            usingPs.add(i);
        }
        synchronized (mPidsSelfLocked) {
            for (ProcessRecord record : mPidsSelfLocked) {
                usingPs.add(record.bpid);
            }
        }
        for (int i = 0; i < ProxyManifest.FREE_COUNT; i++) {
            if (usingPs.contains(i)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static boolean isAttached(ProcessRecord record) {
        IBActivityThread thread = record.bActivityThread;
        return thread != null && thread.asBinder().isBinderAlive();
    }

    private boolean isMappedLocked(ProcessRecord record) {
        synchronized (mProcessMap) {
            Map<String, ProcessRecord> map = mProcessMap.get(BUserHandle.getUid(record.userId, record.buid));
            return map != null && map.get(record.processName) == record;
        }
    }

    private void forgetLocked(ProcessRecord record) {
        int uid = BUserHandle.getUid(record.userId, record.buid);
        synchronized (mProcessMap) {
            Map<String, ProcessRecord> map = mProcessMap.get(uid);
            if (map != null && map.get(record.processName) == record) {
                map.remove(record.processName);
                if (map.isEmpty()) {
                    mProcessMap.remove(uid);
                }
            }
        }
        synchronized (mPidsSelfLocked) {
            mPidsSelfLocked.remove(record);
        }
    }

    public void restartAppProcess(String packageName, String processName, int userId) {
        synchronized (mProcessLock) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            ProcessRecord app = findProcessByPid(callingPid);;
            if (app == null) {
                String stubProcessName = getProcessName(BlackBoxCore.getContext(), callingPid);
                int bpid = parseBPid(stubProcessName);
                startProcessLocked(packageName, processName, userId, bpid, callingPid);
            }
        }
    }

    private int parseBPid(String stubProcessName) {
        String prefix;
        if (stubProcessName == null) {
            return -1;
        } else {
            prefix = BlackBoxCore.getHostPkg() + ":p";
        }
        if (stubProcessName.startsWith(prefix)) {
            try {
                return Integer.parseInt(stubProcessName.substring(prefix.length()));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return -1;
    }

    private boolean initAppProcessL(ProcessRecord record) {
        Log.d(TAG, "initProcess: " + record.processName);
        AppConfig appConfig = record.getClientConfig();
        Bundle bundle = new Bundle();
        bundle.putParcelable(AppConfig.KEY, appConfig);
        Bundle init = ProviderCall.callSafely(record.getProviderAuthority(), "_Black_|_init_process_", null, bundle);
        IBinder appThread = BundleCompat.getBinder(init, "_Black_|_client_");
        if (appThread == null || !appThread.isBinderAlive()) {
            return false;
        }
        attachClientL(record, appThread);

        createProc(record);
        return true;
    }

    private void attachClientL(final ProcessRecord app, final IBinder appThread) {
        IBActivityThread activityThread = IBActivityThread.Stub.asInterface(appThread);
        if (activityThread == null) {
            app.kill();
            return;
        }
        try {
            appThread.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    Log.d(TAG, "App Died: " + app.processName);
                    appThread.unlinkToDeath(this, 0);
                    onProcessDie(app);
                }
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        app.bActivityThread = activityThread;
        try {
            app.appThread = ApplicationThreadCompat.asInterface(activityThread.getActivityThread());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        app.initLock.open();
    }

    public void onProcessDie(ProcessRecord record) {
        if (record.pid > 0) {
            NativeCore.unregisterGuestPid(record.pid);
        }
        synchronized (mProcessLock) {
            record.kill();
            forgetLocked(record);
            boolean bpidStillUsed = false;
            synchronized (mPidsSelfLocked) {
                for (ProcessRecord other : mPidsSelfLocked) {
                    if (other.bpid == record.bpid) {
                        bpidStillUsed = true;
                        break;
                    }
                }
            }
            if (!bpidStillUsed) {
                removeProc(record);
            }
            BNotificationManagerService.get().deletePackageNotification(record.getPackageName(), record.userId);
        }
    }

    public ProcessRecord findProcessRecord(String packageName, String processName, int userId) {
        synchronized (mProcessMap) {
            int appId = BPackageManagerService.get().getAppId(packageName);
            int buid = BUserHandle.getUid(userId, appId);
            Map<String, ProcessRecord> processRecordMap = mProcessMap.get(buid);
            if (processRecordMap == null)
                return null;
            return processRecordMap.get(processName);
        }
    }

    public void killAllByPackageName(String packageName) {
        synchronized (mProcessLock) {
            synchronized (mPidsSelfLocked) {
                List<ProcessRecord> tmp = new ArrayList<>(mPidsSelfLocked);
                int appId = BPackageManagerService.get().getAppId(packageName);
                for (ProcessRecord processRecord : mPidsSelfLocked) {
                    int appId1 = BUserHandle.getAppId(processRecord.buid);
                    if (appId == appId1) {
                        mProcessMap.remove(processRecord.buid);
                        tmp.remove(processRecord);
                        processRecord.kill();
                    }
                }
                mPidsSelfLocked.clear();
                mPidsSelfLocked.addAll(tmp);
            }
        }
    }

    public void killPackageAsUser(String packageName, int userId) {
        synchronized (mProcessLock) {
            int buid = BUserHandle.getUid(userId, BPackageManagerService.get().getAppId(packageName));
            Map<String, ProcessRecord> process = mProcessMap.get(buid);
            if (process == null)
                return;
            for (ProcessRecord value : process.values()) {
                value.kill();
                mPidsSelfLocked.remove(value);
            }
            mProcessMap.remove(buid);
        }
    }

    public List<ProcessRecord> getPackageProcessAsUser(String packageName, int userId) {
        synchronized (mProcessMap) {
            int buid = BUserHandle.getUid(userId, BPackageManagerService.get().getAppId(packageName));
            Map<String, ProcessRecord> process = mProcessMap.get(buid);
            if (process == null)
                return new ArrayList<>();
            return new ArrayList<>(process.values());
        }
    }

    public int getBUidByPidOrPackageName(int pid, String packageName) {
        ProcessRecord callingProcess = findProcessByPid(pid);
        if (callingProcess == null) {
            return BPackageManagerService.get().getAppId(packageName);
        }
        return BUserHandle.getAppId(callingProcess.buid);
    }

    public int getUserIdByCallingPid(int callingPid) {
        ProcessRecord callingProcess = findProcessByPid(callingPid);
        if (callingProcess == null) {
            return 0;
        }
        return callingProcess.userId;
    }

    public ProcessRecord findProcessByPid(int pid) {
        synchronized (mPidsSelfLocked) {
            for (ProcessRecord processRecord : mPidsSelfLocked) {
                if (processRecord.pid == pid)
                    return processRecord;
            }
            return null;
        }
    }

    private static String getProcessName(Context context, int pid) {
        String processName = null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == pid) {
                processName = info.processName;
                break;
            }
        }
        if (processName == null) {
            throw new RuntimeException("processName = null");
        }
        return processName;
    }

    public static int getPid(Context context, String processName) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
                if (runningAppProcess.processName.equals(processName)) {
                    return runningAppProcess.pid;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void createProc(ProcessRecord record) {
        File cmdline = new File(BEnvironment.getProcDir(record.bpid), "cmdline");
        try {
            FileUtils.writeToFile(record.processName.getBytes(), cmdline);
        } catch (IOException ignored) {
        }
    }

    private static void removeProc(ProcessRecord record) {
        FileUtils.deleteDir(BEnvironment.getProcDir(record.bpid));
    }

    @Override
    public void systemReady() {
        FileUtils.deleteDir(BEnvironment.getProcDir());
    }
}
