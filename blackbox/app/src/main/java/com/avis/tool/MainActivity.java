package com.avis.tool;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public class MainActivity extends Activity {
    private static final String PKG = "com.catsbit.oxidesurvivalisland";
    private static final String KEY = "Avis";
    private static final int USER = 0;
    private static final int REQ_PICK = 1;

    private SharedPreferences prefs;
    private View screenKey;
    private View screenGame;
    private EditText keyInput;
    private TextView keyError;
    private Button gameButton;
    private TextView gameStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("avis", MODE_PRIVATE);

        roundCorners(findViewById(R.id.logo), 10);
        roundCorners(findViewById(R.id.gameIcon), 14);

        screenKey = findViewById(R.id.screenKey);
        screenGame = findViewById(R.id.screenGame);
        keyInput = findViewById(R.id.keyInput);
        keyError = findViewById(R.id.keyError);
        gameButton = findViewById(R.id.gameButton);
        gameStatus = findViewById(R.id.gameStatus);

        findViewById(R.id.keyButton).setOnClickListener(v -> submitKey());
        keyInput.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                submitKey();
                return true;
            }
            return false;
        });
        gameButton.setOnClickListener(v -> onGameButton());

        // Long-press the header to dump logcat into Download/avis_tool.log.
        findViewById(R.id.header).setOnLongClickListener(v -> {
            new Thread(() -> {
                Logger.dumpLogcat();
                runOnUiThread(() -> gameStatus.setText("Log saved to Download"));
            }).start();
            return true;
        });

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Logger.log("UNCAUGHT in " + t.getName(), e);
            if (prev != null) prev.uncaughtException(t, e);
        });
        Logger.log("start: sdk=" + Build.VERSION.SDK_INT + " abi=" + Build.SUPPORTED_ABIS[0]
                + " device=" + Build.MODEL);

        if (KEY.equals(prefs.getString("key", ""))) showGame();
    }

    private static void roundCorners(View v, int dp) {
        final float r = dp * v.getResources().getDisplayMetrics().density;
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        v.setClipToOutline(true);
    }

    private void submitKey() {
        if (KEY.equals(keyInput.getText().toString().trim())) {
            prefs.edit().putString("key", KEY).apply();
            showGame();
        } else {
            keyError.setText("Invalid key");
        }
    }

    private void showGame() {
        screenKey.setVisibility(View.GONE);
        screenGame.setVisibility(View.VISIBLE);
        refresh();
    }

    private void refresh() {
        boolean installed = BlackBoxCore.get().isInstalled(PKG, USER);
        gameButton.setText(installed ? "Launch" : "Install");
    }

    private void onGameButton() {
        if (BlackBoxCore.get().isInstalled(PKG, USER)) {
            boolean ok = BlackBoxCore.get().launchApk(PKG, USER);
            Logger.log("launch: " + ok);
            gameStatus.setText(ok ? "" : "Launch failed");
        } else {
            startInstall();
        }
    }

    /** Uses the Oxide copy installed on the device; falls back to the file picker. */
    private void startInstall() {
        ApplicationInfo ai;
        try {
            ai = getPackageManager().getApplicationInfo(PKG, 0);
        } catch (Exception e) {
            Logger.log("Oxide not installed on device, falling back to picker");
            gameStatus.setText("Select base.apk and split_*.apk");
            pick();
            return;
        }
        final List<String> paths = new ArrayList<>();
        paths.add(ai.sourceDir);
        if (ai.splitSourceDirs != null) Collections.addAll(paths, ai.splitSourceDirs);
        Logger.log("found Oxide: nativeLibDir=" + ai.nativeLibraryDir);
        for (String x : paths) Logger.log("  apk: " + x + " (" + new File(x).length() + " bytes)");
        runInstall(() -> installFiles(paths));
    }

    private void runInstall(java.util.concurrent.Callable<String> job) {
        gameButton.setEnabled(false);
        gameStatus.setText("Installing...");
        new Thread(() -> {
            String msg;
            try {
                msg = job.call();
            } catch (Throwable t) {
                Logger.log("install error", t);
                msg = "Error: " + t.getMessage();
            }
            final String m = msg;
            runOnUiThread(() -> {
                gameButton.setEnabled(true);
                gameStatus.setText(m == null ? "" : m);
                refresh();
            });
        }).start();
    }

    private String installFiles(List<String> paths) throws Exception {
        File dir = newStaging();
        try {
            for (String path : paths) {
                File src = new File(path);
                String name = src.getName();
                if (name.equals("base.apk") || name.startsWith("split_")) {
                    Logger.log("copy " + name);
                    try (InputStream in = new FileInputStream(src)) {
                        copy(in, new File(dir, name));
                    }
                }
            }
            return installStaged(dir);
        } finally {
            deleteRec(dir);
        }
    }

    private String installStaged(File dir) {
        if (!new File(dir, "base.apk").isFile()) return "base.apk not found";
        InstallResult r = BlackBoxCore.get().installSplitsAsUser(dir, USER);
        Logger.log("install result: success=" + r.success + " pkg=" + r.packageName + " msg=" + r.msg);
        if (!r.success) return "Install failed: " + r.msg;
        if (!PKG.equals(r.packageName)) {
            BlackBoxCore.get().uninstallPackageAsUser(r.packageName, USER);
            return "Wrong package: " + r.packageName;
        }
        return null;
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
        final List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++)
                uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        runInstall(() -> {
            File dir = newStaging();
            try {
                for (Uri u : uris) {
                    String name = displayName(u);
                    if (name == null || !name.endsWith(".apk")) continue;
                    if (!name.startsWith("split_")) name = "base.apk";
                    try (InputStream in = getContentResolver().openInputStream(u)) {
                        copy(in, new File(dir, name));
                    }
                }
                return installStaged(dir);
            } finally {
                deleteRec(dir);
            }
        });
    }

    private File newStaging() {
        File dir = new File(getCacheDir(), "staging");
        deleteRec(dir);
        dir.mkdirs();
        return dir;
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

    private static void copy(InputStream in, File to) throws Exception {
        try (OutputStream out = new FileOutputStream(to)) {
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
