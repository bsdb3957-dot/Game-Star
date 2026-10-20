package top.niunaijun.blackbox.fake.service;

import android.os.Build;
import android.os.IBinder;
import androidx.fragment.app.z0;
import java.lang.reflect.Method;
import o5.g;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

public class IMiuiSecurityManagerProxy extends BinderInvocationStub {
    private static final String SERVICE_NAME = "miui.security.SecurityManager";
    public static final String TAG = "MiuiSecurityManagerProxy";

    @ProxyMethod("actuallyCheckPermission")
    public static class ActuallyCheckPermission extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "ActuallyCheckPermission: Granting permission");
            return 0;
        }
    }

    @ProxyMethod("checkAccessControl")
    public static class CheckAccessControl extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "CheckAccessControl: Allowing access");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("checkSmsBlocked")
    public static class CheckSmsBlocked extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "CheckSmsBlocked: Returning false (not blocked)");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("getAppPrivacyStatus")
    public static class GetAppPrivacyStatus extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "GetAppPrivacyStatus: Returning default status for " + (args.length > 0 ? (String) args[0] : "unknown"));
            return 0;
        }
    }

    @ProxyMethod("getAppRunningControlEnabled")
    public static class GetAppRunningControlEnabled extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "GetAppRunningControlEnabled: Returning false (disabled)");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("getCurrentUserId")
    public static class GetCurrentUserId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "GetCurrentUserId: Returning current user ID");
            return 0;
        }
    }

    @ProxyMethod("getIcon")
    public static class GetIcon extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "GetIcon: Returning null icon");
            return null;
        }
    }

    @ProxyMethod("getWakeUpTime")
    public static class GetWakeUpTime extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "GetWakeUpTime: Returning default wake up time");
            return 0L;
        }
    }

    @ProxyMethod("isAllowStartActivity")
    public static class IsAllowStartActivity extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "IsAllowStartActivity: Allowing activity start");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("isAppPermissionControlOpen")
    public static class IsAppPermissionControlOpen extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "IsAppPermissionControlOpen: Returning false (disabled)");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isGameMode")
    public static class IsGameMode extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "IsGameMode: Returning false (not in game mode)");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isTrackWakeUp")
    public static class IsTrackWakeUp extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "IsTrackWakeUp: Returning false (disabled)");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isValidDevice")
    public static class IsValidDevice extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "IsValidDevice: Returning true (valid device)");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("pushNewNotification")
    public static class PushNewNotification extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "PushNewNotification: Bypassing notification push");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("saveIcon")
    public static class SaveIcon extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SaveIcon: Bypassing icon save");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("setAppPermissionControlOpen")
    public static class SetAppPermissionControlOpen extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SetAppPermissionControlOpen: Bypassing permission control setting");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("setAppPrivacyStatus")
    public static class SetAppPrivacyStatus extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SetAppPrivacyStatus: Bypassing privacy status for " + (args.length > 0 ? (String) args[0] : "unknown"));
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("setAppRunningControlEnabled")
    public static class SetAppRunningControlEnabled extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SetAppRunningControlEnabled: Bypassing running control setting");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("setCurrentUserId")
    public static class SetCurrentUserId extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SetCurrentUserId: Bypassing user ID setting");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("setGameMode")
    public static class SetGameMode extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SetGameMode: Bypassing game mode setting");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("setTrackWakeUp")
    public static class SetTrackWakeUp extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SetTrackWakeUp: Bypassing track wake up setting");
            return Boolean.TRUE;
        }
    }

    @ProxyMethod("setWakeUpTime")
    public static class SetWakeUpTime extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IMiuiSecurityManagerProxy.TAG, "SetWakeUpTime: Bypassing wake up time setting");
            return Boolean.TRUE;
        }
    }

    public IMiuiSecurityManagerProxy() {
        super(g.a().getService(SERVICE_NAME));
    }

    @Override
    public Object getWho() {
        IBinder service = g.a().getService(SERVICE_NAME);
        if (service == null) {
            return null;
        }
        try {
            return Class.forName("miui.security.ISecurityManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, service);
        } catch (Exception e) {
            z0.l("Failed to get ISecurityManager interface: ", e, TAG);
            return null;
        }
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService(SERVICE_NAME);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    public boolean isEnable() {
        return BuildCompat.isMIUI() || Build.MANUFACTURER.toLowerCase().contains("xiaomi") || Build.BRAND.toLowerCase().contains("xiaomi") || Build.DISPLAY.toLowerCase().contains("hyperos");
    }

    @Override
    public void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new SetAppPrivacyStatus());
        addMethodHook(new GetAppPrivacyStatus());
        addMethodHook(new SetAppPermissionControlOpen());
        addMethodHook(new IsAppPermissionControlOpen());
        addMethodHook(new SetWakeUpTime());
        addMethodHook(new GetWakeUpTime());
        addMethodHook(new SetTrackWakeUp());
        addMethodHook(new IsTrackWakeUp());
        addMethodHook(new SetGameMode());
        addMethodHook(new IsGameMode());
        addMethodHook(new PushNewNotification());
        addMethodHook(new IsAllowStartActivity());
        addMethodHook(new SetAppRunningControlEnabled());
        addMethodHook(new GetAppRunningControlEnabled());
        addMethodHook(new CheckAccessControl());
        addMethodHook(new SaveIcon());
        addMethodHook(new GetIcon());
        addMethodHook(new IsValidDevice());
        addMethodHook(new ActuallyCheckPermission());
        addMethodHook(new CheckSmsBlocked());
        addMethodHook(new SetCurrentUserId());
        addMethodHook(new GetCurrentUserId());
    }
}
