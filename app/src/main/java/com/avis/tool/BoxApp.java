package com.avis.tool;

import android.app.Application;
import android.content.Context;

import java.io.File;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import top.niunaijun.blackbox.core.NativeCore;

public class BoxApp extends Application {
    private static final String GAME_PKG = "com.catsbit.oxidesurvivalisland";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }
            });
            BlackBoxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
                // The guest game's protection lib kills the process on first load when
                // it detects the box; neutralize its on-disk copy before any guest code runs.
                @Override
                public void beforeCreateApplication(String packageName, String processName,
                                                    Context context, int userId) {
                    if (!GAME_PKG.equals(packageName)) return;
                    File dir = context.getDir("libpatch", Context.MODE_PRIVATE);
                    File loader = new File(dir, "libloader.so");
                    if (!loader.isFile()) return;
                    NativeCore.patchLoaderLib(loader.getAbsolutePath());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BlackBoxCore.get().doCreate();
    }
}
