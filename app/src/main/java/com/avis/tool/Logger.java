package com.avis.tool;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Writes Download/avis_tool.log (falls back to the app's external files dir when Download
 * is not writable yet). In debug builds startAutoCapture() streams logcat of the whole uid,
 * which includes the guest game processes running inside the box.
 */
public final class Logger {
    private static final String TAG = "AvisTool";
    private static final String NAME = "avis_tool.log";
    private static final long MAX_BYTES = 10L << 20;
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static File fallbackDir;
    private static Writer out;
    private static boolean capturing;

    private Logger() {}

    static void init(Context c) {
        fallbackDir = c.getExternalFilesDir(null);
    }

    public static File file() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        dir.mkdirs();
        return new File(dir, NAME);
    }

    /** Call when storage access has just been granted so logging moves to Download. */
    static void reopen() {
        synchronized (LOCK) {
            closeQuietly();
        }
    }

    private static void closeQuietly() {
        if (out == null) return;
        try {
            out.close();
        } catch (IOException ignored) {
        }
        out = null;
    }

    private static Writer writer() {
        if (out != null) return out;
        File[] candidates = {file(), fallbackDir == null ? null : new File(fallbackDir, NAME)};
        for (File f : candidates) {
            if (f == null) continue;
            try {
                if (f.length() > MAX_BYTES) f.delete();
                out = new BufferedWriter(new FileWriter(f, true));
                return out;
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static void write(String s, boolean flush) {
        synchronized (LOCK) {
            Writer w = writer();
            if (w == null) return;
            try {
                w.write(s);
                if (flush) w.flush();
            } catch (IOException e) {
                closeQuietly();
            }
        }
    }

    public static void log(String msg) {
        Log.d(TAG, msg);
        synchronized (LOCK) {
            // While capturing, logcat already delivers this line to the file.
            if (!capturing) write(FMT.format(new Date()) + "  " + msg + "\n", true);
        }
    }

    public static void log(String msg, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        log(msg + "\n" + sw);
    }

    /** Streams logcat into the log file until the process dies. Safe to call repeatedly. */
    public static void startAutoCapture() {
        synchronized (LOCK) {
            if (capturing) return;
            capturing = true;
        }
        write("\n===== session " + FMT.format(new Date()) + " =====\n", true);
        Thread t = new Thread(() -> {
            try {
                Process p = new ProcessBuilder("logcat", "-v", "threadtime", "-T", "1")
                        .redirectErrorStream(true).start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) write(line + "\n", !r.ready());
                }
            } catch (Throwable ignored) {
            } finally {
                synchronized (LOCK) {
                    capturing = false;
                }
            }
        }, "avis-logcat");
        t.setDaemon(true);
        t.start();
    }

    /** One-shot dump of the current logcat buffer (used by long-press on the header). */
    public static void dumpLogcat() {
        try {
            Process p = new ProcessBuilder("logcat", "-d", "-v", "threadtime").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                write("\n===== logcat dump " + FMT.format(new Date()) + " =====\n", false);
                String line;
                while ((line = r.readLine()) != null) write(line + "\n", false);
                write("", true);
            }
        } catch (Throwable t) {
            log("logcat dump failed", t);
        }
    }
}
