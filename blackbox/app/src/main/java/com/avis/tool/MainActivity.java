package com.avis.tool;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;

public class MainActivity extends Activity {
    private static final String KEY = "Avis";
    private static final int REQ_PICK = 1;
    private static final int REQ_STORAGE = 2;

    private enum State { ACCESS, INSTALL, BUSY, OPEN }

    private interface Job {
        String run(Installer installer) throws Exception;
    }

    private SharedPreferences prefs;
    private View screenKey;
    private View screenGame;
    private EditText keyInput;
    private TextView keyError;
    private Button gameButton;
    private TextView gameStatus;
    private ProgressBar gameProgress;

    private boolean busy;
    private boolean askedAccess;
    private boolean hadAccess;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        Logger.init(this);
        prefs = getSharedPreferences("avis", MODE_PRIVATE);

        screenKey = findViewById(R.id.screenKey);
        screenGame = findViewById(R.id.screenGame);
        keyInput = findViewById(R.id.keyInput);
        keyError = findViewById(R.id.keyError);
        gameButton = findViewById(R.id.gameButton);
        gameStatus = findViewById(R.id.gameStatus);
        gameProgress = findViewById(R.id.gameProgress);
        roundCorners(findViewById(R.id.gameIcon), 14);

        findViewById(R.id.keyButton).setOnClickListener(v -> submitKey());
        keyInput.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                submitKey();
                return true;
            }
            return false;
        });
        keyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void afterTextChanged(Editable s) {
                clearKeyError();
            }
        });
        gameButton.setOnClickListener(v -> onGameButton());

        // Long-press the header to dump logcat into Download/avis_tool.log (release builds).
        findViewById(R.id.header).setOnLongClickListener(v -> {
            new Thread(() -> {
                Logger.dumpLogcat();
                runOnUiThread(() -> setStatus("Log saved to Download"));
            }).start();
            return true;
        });

        Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Logger.log("UNCAUGHT in " + t.getName(), e);
            if (prev != null) prev.uncaughtException(t, e);
        });
        Logger.log("start: sdk=" + Build.VERSION.SDK_INT + " abi=" + Build.SUPPORTED_ABIS[0]
                + " device=" + Build.MODEL + " debug=" + BuildConfig.DEBUG);

        if (KEY.equals(prefs.getString("key", ""))) {
            screenKey.setVisibility(View.GONE);
            enterGame(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean access = hasAccess();
        if (access && !hadAccess) Logger.reopen();
        hadAccess = access;
        if (BuildConfig.DEBUG && access) Logger.startAutoCapture();
        if (screenGame.getVisibility() == View.VISIBLE) refresh();
    }

    // ---- key screen ----

    private void submitKey() {
        if (KEY.equals(keyInput.getText().toString().trim())) {
            prefs.edit().putString("key", KEY).apply();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(keyInput.getWindowToken(), 0);
            screenKey.animate().alpha(0f).translationY(-dp(10)).setDuration(160)
                    .withEndAction(() -> {
                        screenKey.setVisibility(View.GONE);
                        enterGame(true);
                    }).start();
        } else {
            keyInput.setBackgroundResource(R.drawable.bg_field_error);
            keyError.setText("Invalid key");
            keyError.setVisibility(View.VISIBLE);
            keyInput.animate().translationX(0).setDuration(0).start();
            android.animation.ObjectAnimator.ofFloat(keyInput, "translationX",
                    0, -dp(10), dp(10), -dp(6), dp(6), 0).setDuration(320).start();
            keyInput.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    private void clearKeyError() {
        if (keyError.getVisibility() == View.VISIBLE) {
            keyError.setVisibility(View.INVISIBLE);
            keyInput.setBackgroundResource(R.drawable.bg_field);
        }
    }

    // ---- game screen ----

    private void enterGame(boolean animate) {
        screenGame.setVisibility(View.VISIBLE);
        if (animate) {
            screenGame.setAlpha(0f);
            screenGame.setTranslationY(dp(16));
            screenGame.animate().alpha(1f).translationY(0).setDuration(280)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        refresh();
        if (!installed() && !hasAccess() && !askedAccess) {
            askedAccess = true;
            requestAccess();
        }
    }

    private boolean installed() {
        return BlackBoxCore.get().isInstalled(Installer.PKG, Installer.USER);
    }

    private State state() {
        if (busy) return State.BUSY;
        if (installed()) return State.OPEN;
        if (!hasAccess()) return State.ACCESS;
        return State.INSTALL;
    }

    private void refresh() {
        State s = state();
        switch (s) {
            case ACCESS:
                gameButton.setText("Allow access");
                break;
            case OPEN:
                gameButton.setText("Open");
                break;
            default:
                gameButton.setText("Install");
        }
        gameButton.setEnabled(s != State.BUSY);
        gameButton.setAlpha(s == State.BUSY ? 0.35f : 1f);
    }

    private void onGameButton() {
        switch (state()) {
            case ACCESS:
                requestAccess();
                break;
            case INSTALL:
                startInstall();
                break;
            case OPEN:
                boolean ok = BlackBoxCore.get().launchApk(Installer.PKG, Installer.USER);
                Logger.log("launch: " + ok);
                setStatus(ok ? null : "Can't open");
                break;
            default:
                break;
        }
    }

    private void startInstall() {
        if (Installer.isOnDevice(this)) {
            runInstall(Installer::fromDevice);
        } else {
            Logger.log("Oxide not installed on device, falling back to picker");
            setStatus("Select base.apk and split_*.apk");
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, REQ_PICK);
        }
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
        runInstall(inst -> inst.fromUris(uris));
    }

    private void runInstall(Job job) {
        busy = true;
        refresh();
        showProgress(-1);
        setStatus("Preparing");
        final Installer.Listener listener = new Installer.Listener() {
            @Override
            public void copying(int permille) {
                runOnUiThread(() -> {
                    showProgress(permille);
                    setStatus(permille < 0 ? "Copying" : "Copying " + permille / 10 + "%");
                });
            }

            @Override
            public void installing() {
                runOnUiThread(() -> {
                    showProgress(-1);
                    setStatus("Installing");
                });
            }
        };
        new Thread(() -> {
            String err;
            try {
                err = job.run(new Installer(MainActivity.this, listener));
            } catch (Throwable t) {
                Logger.log("install error", t);
                err = "Install failed";
            }
            final String msg = err;
            runOnUiThread(() -> {
                busy = false;
                gameProgress.setVisibility(View.GONE);
                setStatus(msg);
                refresh();
                if (msg == null) gameButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            });
        }, "avis-install").start();
    }

    private void showProgress(int permille) {
        gameProgress.setVisibility(View.VISIBLE);
        if (permille < 0) {
            gameProgress.setIndeterminate(true);
        } else {
            gameProgress.setIndeterminate(false);
            gameProgress.setProgress(permille);
        }
    }

    private void setStatus(String s) {
        if (s == null || s.isEmpty()) {
            gameStatus.setVisibility(View.GONE);
        } else {
            gameStatus.setText(s);
            gameStatus.setVisibility(View.VISIBLE);
        }
    }

    // ---- storage access ----

    private boolean hasAccess() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())), REQ_STORAGE);
            } catch (Exception e) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                        REQ_STORAGE);
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
        }
    }

    // ---- helpers ----

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
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
}
