package com.avis.tool;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Writes to Download/avis_tool.log; silently falls back to logcat only if storage is not writable. */
public final class Logger {
    private static final String TAG = "AvisTool";
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private Logger() {}

    public static File file() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        dir.mkdirs();
        return new File(dir, "avis_tool.log");
    }

    public static synchronized void log(String msg) {
        Log.d(TAG, msg);
        try (FileWriter w = new FileWriter(file(), true)) {
            w.write(FMT.format(new Date()) + "  " + msg + "\n");
        } catch (Throwable ignored) {
        }
    }

    public static void log(String msg, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        log(msg + "\n" + sw);
    }

    /** Appends this uid's logcat (host plus box guest processes) to the log file. */
    public static synchronized void dumpLogcat() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "threadtime"});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                 FileWriter w = new FileWriter(file(), true)) {
                w.write("\n===== logcat dump " + FMT.format(new Date()) + " =====\n");
                String line;
                while ((line = r.readLine()) != null) w.write(line + "\n");
            }
        } catch (Throwable t) {
            log("logcat dump failed", t);
        }
    }
}
