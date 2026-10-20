package com.oxide.box;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public class MainActivity extends Activity {
    private static final String PKG = "com.catsbit.oxidesurvivalisland";
    private static final int USER = 0;
    private static final int REQ_PICK = 1;

    private TextView status;
    private Button install;
    private Button launch;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int p = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("Oxide Survival Island");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, p, 0, p);
        root.addView(status);

        install = new Button(this);
        install.setText("Install");
        install.setOnClickListener(v -> start());
        root.addView(install);

        launch = new Button(this);
        launch.setText("Launch");
        launch.setOnClickListener(v -> {
            if (!BlackBoxCore.get().launchApk(PKG, USER)) status.setText("Launch failed");
        });
        root.addView(launch);

        setContentView(root);
        refresh();
    }

    private void refresh() {
        boolean installed = BlackBoxCore.get().isInstalled(PKG, USER);
        status.setText(installed ? "Installed" : "Not installed");
        launch.setVisibility(installed ? View.VISIBLE : View.GONE);
    }

    /** Uses the Oxide copy installed on the device; falls back to the file picker. */
    private void start() {
        android.content.pm.ApplicationInfo ai;
        try {
            ai = getPackageManager().getApplicationInfo(PKG, 0);
        } catch (Exception e) {
            status.setText("Oxide not found on device, select base.apk + split_*.apk");
            pick();
            return;
        }
        final java.util.List<String> paths = new java.util.ArrayList<>();
        paths.add(ai.sourceDir);
        if (ai.splitSourceDirs != null) java.util.Collections.addAll(paths, ai.splitSourceDirs);
        install.setEnabled(false);
        status.setText("Installing...");
        new Thread(() -> {
            String msg = installFiles(paths);
            runOnUiThread(() -> {
                install.setEnabled(true);
                refresh();
                if (msg != null) status.setText(msg);
            });
        }).start();
    }

    private String installFiles(java.util.List<String> paths) {
        File dir = new File(getCacheDir(), "oxide_splits");
        deleteRec(dir);
        dir.mkdirs();
        try {
            for (String path : paths) {
                File src = new File(path);
                String name = src.getName();
                if (name.equals("base.apk") || name.startsWith("split_")) {
                    try (InputStream in = new java.io.FileInputStream(src);
                         OutputStream out = new FileOutputStream(new File(dir, name))) {
                        byte[] buf = new byte[1 << 16];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    }
                }
            }
            if (!new File(dir, "base.apk").isFile()) return "base.apk not found";
            InstallResult r = BlackBoxCore.get().installSplitsAsUser(dir, USER);
            return r.success ? null : "Install failed: " + r.msg;
        } catch (Throwable t) {
            return "Error: " + t;
        } finally {
            deleteRec(dir);
        }
    }

    private void pick() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PICK || res != RESULT_OK || data == null) return;
        java.util.List<Uri> uris = new java.util.ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++)
                uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        install.setEnabled(false);
        status.setText("Installing...");
        new Thread(() -> {
            String msg = doInstall(uris);
            runOnUiThread(() -> {
                install.setEnabled(true);
                refresh();
                if (msg != null) status.setText(msg);
            });
        }).start();
    }

    /** Returns an error message, or null on success. */
    private String doInstall(java.util.List<Uri> uris) {
        File dir = new File(getCacheDir(), "oxide_splits");
        deleteRec(dir);
        dir.mkdirs();
        try {
            int unnamedBase = 0;
            for (Uri u : uris) {
                String name = displayName(u);
                if (name == null || !name.endsWith(".apk")) continue;
                if (!name.startsWith("split_") && !name.equals("base.apk")) {
                    name = "base.apk";
                    unnamedBase++;
                }
                copy(u, new File(dir, name));
            }
            if (unnamedBase > 1) return "Select exactly one base APK";
            if (!new File(dir, "base.apk").isFile()) return "base.apk not selected";
            InstallResult r = BlackBoxCore.get().installSplitsAsUser(dir, USER);
            if (!r.success) return "Install failed: " + r.msg;
            if (!PKG.equals(r.packageName)) {
                BlackBoxCore.get().uninstallPackageAsUser(r.packageName, USER);
                return "Not an Oxide Survival Island package: " + r.packageName;
            }
            return null;
        } catch (Throwable t) {
            return "Error: " + t;
        } finally {
            deleteRec(dir);
        }
    }

    private String displayName(Uri u) {
        try (android.database.Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        }
        return u.getLastPathSegment();
    }

    private void copy(Uri from, File to) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static void deleteRec(File f) {
        File[] c = f.listFiles();
        if (c != null) for (File x : c) deleteRec(x);
        f.delete();
    }
}
