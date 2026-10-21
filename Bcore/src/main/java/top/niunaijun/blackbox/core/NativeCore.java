package top.niunaijun.blackbox.core;


import android.os.Process;

import androidx.annotation.Keep;

import java.io.File;
import java.util.List;

import dalvik.system.DexFile;
import android.os.Binder;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.utils.compat.DexFileCompat;
import top.niunaijun.blackbox.utils.Slog;

import static top.niunaijun.blackbox.core.env.BEnvironment.EMPTY_JAR;

/**
 * Created by Milk on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class NativeCore {
    public static final String TAG = "NativeCore";

    static {
        new File("");
        System.loadLibrary("ABridge");
    }

    public static native void init(int apiLevel);

    public static native void enableIO();

    public static native void addIORule(String targetPath, String relocatePath);

    public static native void patchLoaderLib(String path);

    public static native boolean checkTailPatchFailed();

    public static native void hideXposed();

    /**
     * The native side keeps a pid -> virtual uid map plus a per-process
     * "self" guest uid; together they let the Binder hook report the
     * guest's virtual uid instead of the host app uid.
     */
    public static void registerGuestPid(int pid, int uid) {
        try {
            pidMapPut(pidMapPath(), pid, uid);
        } catch (Throwable t) {
            Slog.w(TAG, "could not register guest pid " + pid, t);
        }
    }

    public static void unregisterGuestPid(int pid) {
        try {
            pidMapRemove(pidMapPath(), pid);
        } catch (Throwable t) {
            Slog.w(TAG, "could not unregister guest pid " + pid, t);
        }
    }

    public static void setGuestSelfUid(int uid) {
        try {
            setSelfUid(uid);
        } catch (Throwable t) {
            Slog.w(TAG, "could not set guest self uid " + uid, t);
        }
    }

    private static String pidMapPath() {
        return new File(BEnvironment.getSystemDir(), "pidmap").getAbsolutePath();
    }

    private static native void pidMapPut(String path, int pid, int uid);

    private static native void pidMapRemove(String path, int pid);

    private static native void setSelfUid(int uid);

    public static void dumpDex(ClassLoader classLoader, String packageName) {
        List<Long> cookies = DexFileCompat.getCookies(classLoader);
        for (Long cookie : cookies) {
            if (cookie == 0)
                continue;
//            File file = new File(BlackBoxCore.get().getDexDumpDir(), packageName);
//            FileUtils.mkdirs(file);
//            dumpDex(cookie, file.getAbsolutePath());
        }
    }

    @Keep
    public static int getCallingUid(int origCallingUid) {
        // 系统uid
        if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
            return origCallingUid;
        // 非用户应用
        if (origCallingUid > Process.LAST_APPLICATION_UID)
            return origCallingUid;

        if (origCallingUid != BlackBoxCore.getHostUid()) {
            return origCallingUid;
        }

        try {
            int callingPid = Binder.getCallingPid();
            if (callingPid <= 0 || callingPid == Process.myPid()) {
                return Process.myUid();
            }
            int bUidByPid = BlackBoxCore.getBActivityManager().getBUidByPid(callingPid);
            if (bUidByPid > 0) {
                return bUidByPid;
            }
            return Process.SYSTEM_UID;
        } catch (Throwable e) {
            Slog.w(TAG, "could not resolve calling uid, falling back to host uid", e);
            return Process.myUid();
        }
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static long[] loadEmptyDex() {
        try {
            DexFile dexFile = new DexFile(EMPTY_JAR);
            List<Long> cookies = DexFileCompat.getCookies(dexFile);
            long[] longs = new long[cookies.size()];
            for (int i = 0; i < cookies.size(); i++) {
                longs[i] = cookies.get(i);
            }
            return longs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new long[]{};
    }
}
