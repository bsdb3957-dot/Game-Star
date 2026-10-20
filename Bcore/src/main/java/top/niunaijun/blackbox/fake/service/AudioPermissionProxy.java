package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.IBinder;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class AudioPermissionProxy extends BinderInvocationStub {
    public static final String TAG = "AudioPermissionProxy";

    @ProxyMethod("abandonAudioFocus")
    public static class AbandonAudioFocus extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: abandonAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getMode")
    public static class GetMode extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: getMode called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getRecordingState")
    public static class GetRecordingState extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: getRecordingState called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getRecordingStateForUser")
    public static class GetRecordingStateForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: getRecordingStateForUser called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getStreamMaxVolume")
    public static class GetStreamMaxVolume extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: getStreamMaxVolume called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getStreamVolume")
    public static class GetStreamVolume extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: getStreamVolume called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isBluetoothScoOn")
    public static class IsBluetoothScoOn extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: isBluetoothScoOn called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isMicrophoneMuted")
    public static class IsMicrophoneMuted extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: isMicrophoneMuted returning false");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isMicrophoneMutedForUser")
    public static class IsMicrophoneMutedForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: isMicrophoneMutedForUser returning false");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isRecordingActive")
    public static class IsRecordingActive extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: isRecordingActive called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isSpeakerphoneOn")
    public static class IsSpeakerphoneOn extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: isSpeakerphoneOn called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isStreamMute")
    public static class IsStreamMute extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: isStreamMute called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("registerAudioFocusClient")
    public static class RegisterAudioFocusClient extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: registerAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("requestAudioFocus")
    public static class RequestAudioFocus extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: requestAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setBluetoothScoOn")
    public static class SetBluetoothScoOn extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: setBluetoothScoOn called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setMicrophoneMute")
    public static class SetMicrophoneMute extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: setMicrophoneMute called, forcing unmute");
            if (args != null && args.length > 0) {
                args[0] = Boolean.FALSE;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setMicrophoneMuteForUser")
    public static class SetMicrophoneMuteForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: setMicrophoneMuteForUser called, forcing unmute");
            if (args != null && args.length > 1) {
                args[1] = Boolean.FALSE;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setMode")
    public static class SetMode extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: setMode called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setSpeakerphoneOn")
    public static class SetSpeakerphoneOn extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: setSpeakerphoneOn called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setStreamMute")
    public static class SetStreamMute extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: setStreamMute called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setStreamVolume")
    public static class SetStreamVolume extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: setStreamVolume called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("startRecording")
    public static class StartRecording extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: startRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("stopRecording")
    public static class StopRecording extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: stopRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("unregisterAudioFocusClient")
    public static class UnregisterAudioFocusClient extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(AudioPermissionProxy.TAG, "AudioPermission: unregisterAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    public AudioPermissionProxy() {
        super(BRServiceManager.get().getService("audio"));
    }

    @Override
    public Object getWho() {
        IBinder service = BRServiceManager.get().getService("audio");
        if (service == null) {
            Slog.e(TAG, "Failed to get audio service binder");
            return null;
        }
        try {
            Object objInvoke = Class.forName("android.media.IAudioService$Stub").getMethod("asInterface", IBinder.class).invoke(null, service);
            if (objInvoke != null) {
                Slog.d(TAG, "Successfully obtained IAudioService interface");
                return objInvoke;
            }
            Slog.e(TAG, "Reflection succeeded but returned null interface");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IAudioService interface", e);
            return null;
        }
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService("audio");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
