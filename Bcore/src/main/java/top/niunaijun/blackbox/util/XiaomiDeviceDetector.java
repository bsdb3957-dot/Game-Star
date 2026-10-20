package top.niunaijun.blackbox.util;

import android.os.Build;
import android.text.TextUtils;
import top.niunaijun.blackbox.utils.ShellUtils;
import top.niunaijun.blackbox.utils.Slog;

public class XiaomiDeviceDetector {
    private static final String TAG = "XiaomiDeviceDetector";
    private static final String[] XIAOMI_MANUFACTURERS = {"Xiaomi", "Redmi", "POCO", "Black Shark", "Mi"};
    private static final String[] MIUI_VERSION_PATTERNS = {"MIUI", "HyperOS", "MIUI Global", "MIUI China"};
    private static final String[] XIAOMI_MODELS = {"Mi ", "Redmi ", "POCO ", "Black Shark ", "Xiaomi "};
    private static boolean sIsXiaomiDevice = false;
    private static String sMiuiVersion = null;
    private static String sDeviceModel = null;
    private static int sAndroidVersion = 0;

    private static boolean checkXiaomiSystemProperties() {
        try {
            return (TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name")) && TextUtils.isEmpty(getSystemProperty("ro.miui.version.code")) && TextUtils.isEmpty(getSystemProperty("ro.miui.build.version"))) ? false : true;
        } catch (Exception e) {
            Slog.w(TAG, "Error checking Xiaomi system properties: " + e.getMessage());
            return false;
        }
    }

    private static boolean containsMiuiVersion(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String lowerCase = str.toLowerCase();
        for (String str2 : MIUI_VERSION_PATTERNS) {
            if (lowerCase.contains(str2.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String extractMiuiVersion(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] strArrSplit = str.split(" ");
        for (int i = 0; i < strArrSplit.length - 1; i++) {
            if (strArrSplit[i].equalsIgnoreCase("MIUI") || strArrSplit[i].equalsIgnoreCase("HyperOS")) {
                return strArrSplit[i] + " " + strArrSplit[i + 1];
            }
        }
        return str;
    }

    private static String extractVersionNumber(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] strArrSplit = str.split(" ");
        if (strArrSplit.length > 1) {
            String[] strArrSplit2 = strArrSplit[1].split("\\.");
            if (strArrSplit2.length > 0) {
                return strArrSplit2[0];
            }
        }
        return null;
    }

    public static int getAndroidVersion() {
        int i = sAndroidVersion;
        if (i > 0) {
            return i;
        }
        int i = Build.VERSION.SDK_INT;
        sAndroidVersion = i;
        return i;
    }

    public static String getDeviceInfo() {
        StringBuilder sb = new StringBuilder("Device: ");
        sb.append(Build.MANUFACTURER);
        sb.append(" ");
        sb.append(Build.MODEL);
        sb.append("\nAndroid: ");
        sb.append(Build.VERSION.RELEASE);
        sb.append(" (API ");
        sb.append(Build.VERSION.SDK_INT);
        sb.append(")\nXiaomi Device: ");
        sb.append(isXiaomiDevice());
        sb.append(ShellUtils.COMMAND_LINE_END);
        if (isXiaomiDevice()) {
            sb.append("MIUI Version: ");
            sb.append(getMiuiVersion());
            sb.append("\nMIUI 12+: ");
            sb.append(isMiui12OrHigher());
            sb.append("\nMIUI 13+: ");
            sb.append(isMiui13OrHigher());
            sb.append("\nHyperOS: ");
            sb.append(isHyperOS());
            sb.append(ShellUtils.COMMAND_LINE_END);
        }
        return sb.toString();
    }

    public static String getDeviceModel() {
        String str = sDeviceModel;
        if (str != null) {
            return str;
        }
        String str2 = Build.MODEL;
        sDeviceModel = str2;
        return str2;
    }

    public static String getMiuiVersion() {
        String str = sMiuiVersion;
        if (str != null) {
            return str;
        }
        if (!isXiaomiDevice()) {
            return null;
        }
        try {
            String systemProperty = getSystemProperty("ro.miui.ui.version.name");
            sMiuiVersion = systemProperty;
            if (!TextUtils.isEmpty(systemProperty)) {
                Slog.d(TAG, "Detected MIUI version: " + sMiuiVersion);
                return sMiuiVersion;
            }
            String systemProperty2 = getSystemProperty("ro.miui.version.code");
            sMiuiVersion = systemProperty2;
            if (!TextUtils.isEmpty(systemProperty2)) {
                Slog.d(TAG, "Detected MIUI version code: " + sMiuiVersion);
                return sMiuiVersion;
            }
            String str2 = Build.DISPLAY;
            if (!TextUtils.isEmpty(str2) && containsMiuiVersion(str2)) {
                sMiuiVersion = extractMiuiVersion(str2);
                Slog.d(TAG, "Detected MIUI version from build: " + sMiuiVersion);
                return sMiuiVersion;
            }
            sMiuiVersion = "Unknown";
            return "Unknown";
        } catch (Exception e) {
            Slog.w(TAG, "Error getting MIUI version: " + e.getMessage());
        }
    }

    private static String getSystemProperty(String str) {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, str);
        } catch (Exception e) {
            Slog.w(TAG, a.a("Error getting system property ", str, ": ").append(e.getMessage()).toString());
            return null;
        }
    }

    public static boolean isHyperOS() {
        if (!isXiaomiDevice()) {
            return false;
        }
        String miuiVersion = getMiuiVersion();
        if (TextUtils.isEmpty(miuiVersion) || "Unknown".equals(miuiVersion)) {
            return false;
        }
        return miuiVersion.contains("HyperOS") || miuiVersion.contains("OS1.");
    }

    public static boolean isMiui12OrHigher() {
        if (!isXiaomiDevice()) {
            return false;
        }
        String miuiVersion = getMiuiVersion();
        if (!TextUtils.isEmpty(miuiVersion) && !"Unknown".equals(miuiVersion)) {
            try {
                String strExtractVersionNumber = extractVersionNumber(miuiVersion);
                return !TextUtils.isEmpty(strExtractVersionNumber) && Integer.parseInt(strExtractVersionNumber) >= 12;
            } catch (Exception e) {
                Slog.w(TAG, "Error checking MIUI version: " + e.getMessage());
            }
        }
        return false;
    }

    public static boolean isMiui13OrHigher() {
        if (!isXiaomiDevice()) {
            return false;
        }
        String miuiVersion = getMiuiVersion();
        if (!TextUtils.isEmpty(miuiVersion) && !"Unknown".equals(miuiVersion)) {
            try {
                String strExtractVersionNumber = extractVersionNumber(miuiVersion);
                return !TextUtils.isEmpty(strExtractVersionNumber) && Integer.parseInt(strExtractVersionNumber) >= 13;
            } catch (Exception e) {
                Slog.w(TAG, "Error checking MIUI version: " + e.getMessage());
            }
        }
        return false;
    }

    public static boolean isXiaomiDevice() {
        if (sIsXiaomiDevice) {
            return true;
        }
        try {
            String str = Build.MANUFACTURER;
            if (isXiaomiManufacturer(str)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by manufacturer: " + str);
                return true;
            }
            String str2 = Build.BRAND;
            if (isXiaomiManufacturer(str2)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by brand: " + str2);
                return true;
            }
            String str3 = Build.MODEL;
            if (isXiaomiModel(str3)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by model: " + str3);
                return true;
            }
            String str4 = Build.PRODUCT;
            if (isXiaomiModel(str4)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by product: " + str4);
                return true;
            }
            String str5 = Build.DEVICE;
            if (isXiaomiModel(str5)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by device: " + str5);
                return true;
            }
            String str6 = Build.FINGERPRINT;
            if (isXiaomiFingerprint(str6)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by fingerprint: " + str6);
                return true;
            }
            if (checkXiaomiSystemProperties()) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by system properties");
                return true;
            }
            sIsXiaomiDevice = false;
            return false;
        } catch (Exception e) {
            Slog.w(TAG, "Error detecting Xiaomi device: " + e.getMessage());
        }
    }

    private static boolean isXiaomiFingerprint(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String lowerCase = str.toLowerCase();
        return lowerCase.contains("xiaomi") || lowerCase.contains("redmi") || lowerCase.contains("poco") || lowerCase.contains("blackshark") || lowerCase.contains("mi ");
    }

    private static boolean isXiaomiManufacturer(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String lowerCase = str.toLowerCase();
        for (String str2 : XIAOMI_MANUFACTURERS) {
            if (lowerCase.contains(str2.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isXiaomiModel(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String lowerCase = str.toLowerCase();
        for (String str2 : XIAOMI_MODELS) {
            if (lowerCase.contains(str2.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
