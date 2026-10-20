package top.niunaijun.blackbox.utils;

import java.security.MessageDigest;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.GmsCore;

public final class GuestIds {
    private GuestIds() {
    }

    public static String appSetId() {
        String strSeed = seed();
        if (strSeed == null) {
            return null;
        }
        return uuid(digest(strSeed, "appset"));
    }

    private static byte[] digest(String str, String str2) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes());
            messageDigest.update(str2.getBytes());
            return messageDigest.digest();
        } catch (Throwable unused) {
            byte[] bArr = new byte[32];
            byte[] bytes = (str + str2).getBytes();
            System.arraycopy(bytes, 0, bArr, 0, Math.min(bytes.length, 32));
            return bArr;
        }
    }

    public static String gaid() {
        String strSeed = seed();
        if (strSeed == null) {
            return null;
        }
        return uuid(digest(strSeed, "gaid"));
    }

    private static String hex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < length && index < bytes.length; index++) {
            sb.append(String.format("%02x", Byte.valueOf(bytes[index])));
        }
        return sb.toString();
    }

    public static String mac() {
        String strSeed = seed();
        if (strSeed == null) {
            return null;
        }
        byte[] bArrDigest = digest(strSeed, "mac");
        bArrDigest[0] = (byte) ((bArrDigest[0] | 2) & 254);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02x", Byte.valueOf(bArrDigest[i])));
        }
        return sb.toString();
    }

    public static String seed() {
        String guestAndroidId;
        try {
            String appPackageName = BActivityThread.getAppPackageName();
            if (appPackageName == null || GmsCore.isGoogleAppOrService(appPackageName) || (guestAndroidId = BlackBoxCore.getBActivityManager().getGuestAndroidId(BActivityThread.getUserId(), appPackageName)) == null || guestAndroidId.isEmpty()) {
                return null;
            }
            return guestAndroidId;
        } catch (Throwable unused) {
            return null;
        }
    }

    public static String serial() {
        String strSeed = seed();
        if (strSeed == null) {
            return null;
        }
        return hex(digest(strSeed, "serial"), 8).toUpperCase();
    }

    private static String uuid(byte[] bArr) {
        bArr[6] = (byte) ((bArr[6] & 15) | 64);
        bArr[8] = (byte) ((bArr[8] & 63) | 128);
        String strHex = hex(bArr, 16);
        return strHex.substring(0, 8) + "-" + strHex.substring(8, 12) + "-" + strHex.substring(12, 16) + "-" + strHex.substring(16, 20) + "-" + strHex.substring(20, 32);
    }

    public static byte[] widevineId() {
        String strSeed = seed();
        if (strSeed == null) {
            return null;
        }
        return digest(strSeed, "widevine");
    }
}
