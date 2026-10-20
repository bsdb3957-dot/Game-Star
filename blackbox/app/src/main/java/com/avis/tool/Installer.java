package com.avis.tool;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

/** Copies base.apk + split_*.apk into a staging dir and hands them to the box. */
final class Installer {
    static final String PKG = "com.catsbit.oxidesurvivalisland";
    static final int USER = 0;

    interface Listener {
        /** permille 0..1000, or -1 when the total is unknown. */
        void copying(int permille);

        void installing();
    }

    private final Context ctx;
    private final Listener listener;

    Installer(Context ctx, Listener listener) {
        this.ctx = ctx;
        this.listener = listener;
    }

    static boolean isOnDevice(Context c) {
        try {
            c.getPackageManager().getApplicationInfo(PKG, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Installs the copy that is already on the phone (paths come from PackageManager). */
    String fromDevice() throws Exception {
        ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(PKG, 0);
        List<File> files = new ArrayList<>();
        files.add(new File(ai.sourceDir));
        if (ai.splitSourceDirs != null) {
            for (String s : ai.splitSourceDirs) files.add(new File(s));
        }
        Logger.log("found Oxide: nativeLibDir=" + ai.nativeLibraryDir);
        long total = 0;
        for (File f : files) {
            Logger.log("  apk: " + f.getAbsolutePath() + " (" + f.length() + " bytes)");
            if (wanted(f.getName())) total += f.length();
        }
        File dir = staging();
        try {
            long[] done = {0};
            for (File f : files) {
                if (!wanted(f.getName())) continue;
                Logger.log("copy " + f.getName());
                try (InputStream in = new FileInputStream(f)) {
                    copy(in, new File(dir, f.getName()), done, total);
                }
            }
            return installStaged(dir);
        } finally {
            deleteRec(dir);
        }
    }

    /** Installs files picked by the user; names that are not split_* are treated as the base. */
    String fromUris(List<Uri> uris) throws Exception {
        File dir = staging();
        try {
            long[] done = {0};
            for (Uri u : uris) {
                String name = displayName(u);
                if (name == null || !name.endsWith(".apk")) continue;
                if (!name.startsWith("split_")) name = "base.apk";
                Logger.log("copy " + name);
                try (InputStream in = ctx.getContentResolver().openInputStream(u)) {
                    copy(in, new File(dir, name), done, -1);
                }
            }
            return installStaged(dir);
        } finally {
            deleteRec(dir);
        }
    }

    private String installStaged(File dir) {
        if (!new File(dir, "base.apk").isFile()) return "base.apk not found";
        listener.installing();
        InstallResult r = BlackBoxCore.get().installSplitsAsUser(dir, USER);
        Logger.log("install result: success=" + r.success + " pkg=" + r.packageName + " msg=" + r.msg);
        if (!r.success) return "Install failed";
        if (!PKG.equals(r.packageName)) {
            BlackBoxCore.get().uninstallPackageAsUser(r.packageName, USER);
            return "Wrong package";
        }
        return null;
    }

    private static boolean wanted(String name) {
        return name.equals("base.apk") || (name.startsWith("split_") && name.endsWith(".apk"));
    }

    private File staging() {
        File dir = new File(ctx.getCacheDir(), "staging");
        deleteRec(dir);
        dir.mkdirs();
        return dir;
    }

    private String displayName(Uri u) {
        try (android.database.Cursor c = ctx.getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        }
        return u.getLastPathSegment();
    }

    private void copy(InputStream in, File to, long[] done, long total) throws Exception {
        long last = 0;
        try (OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[1 << 17];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                done[0] += n;
                long now = System.currentTimeMillis();
                if (now - last > 120) {
                    last = now;
                    listener.copying(total > 0 ? (int) (done[0] * 1000 / total) : -1);
                }
            }
        }
    }

    private static void deleteRec(File f) {
        File[] c = f.listFiles();
        if (c != null) for (File x : c) deleteRec(x);
        f.delete();
    }
}
