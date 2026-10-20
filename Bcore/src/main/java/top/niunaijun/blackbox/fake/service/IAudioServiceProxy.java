package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.IBinder;
import android.os.IInterface;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;

public class IAudioServiceProxy extends BinderInvocationStub {
    public static final String TAG = "AudioServiceProxy";

    @ProxyMethod("abandonAudioFocus")
    public static class AbandonAudioFocus extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: abandonAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getRecordingState")
    public static class GetRecordingState extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: getRecordingState called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getRecordingStateForUser")
    public static class GetRecordingStateForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: getRecordingStateForUser called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isMicrophoneMuted")
    public static class IsMicrophoneMuted extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: isMicrophoneMuted returning false");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isMicrophoneMutedForUser")
    public static class IsMicrophoneMutedForUser extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: isMicrophoneMutedForUser returning false");
            return Boolean.FALSE;
        }
    }

    @ProxyMethod("isRecordingActive")
    public static class IsRecordingActive extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: isRecordingActive called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("registerAudioFocusClient")
    public static class RegisterAudioFocusClient extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: registerAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("requestAudioFocus")
    public static class RequestAudioFocus extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: requestAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setMicrophoneMute")
    public static class SetMicrophoneMute extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: setMicrophoneMute called, forcing unmute");
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
            Slog.d(IAudioServiceProxy.TAG, "AudioService: setMicrophoneMuteForUser called, forcing unmute");
            if (args != null && args.length > 1) {
                args[1] = Boolean.FALSE;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("startRecording")
    public static class StartRecording extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: startRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("stopRecording")
    public static class StopRecording extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: stopRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("unregisterAudioFocusClient")
    public static class UnregisterAudioFocusClient extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            Slog.d(IAudioServiceProxy.TAG, "AudioService: unregisterAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    public IAudioServiceProxy() {
        super(BRServiceManager.get().getService("audio"));
    }

    @Override
    public Object getWho() {
        Object objInvoke;
        IBinder service = BRServiceManager.get().getService("audio");
        if (service == null) {
            Slog.e(TAG, "Failed to get AUDIO_SERVICE binder");
            return null;
        }
        try {
            try {
                objInvoke = Reflector.on("android.media.IAudioService$Stub").call("asInterface", service);
            } catch (Exception e) {
                Slog.d(TAG, "Failed Android 16+ path, trying alternative: " + e.getMessage());
                try {
                    objInvoke = Reflector.on("android.media.IAudioService").call("asInterface", service);
                } catch (Exception e) {
                    Slog.d(TAG, "Failed alternative path: " + e.getMessage());
                    try {
                        objInvoke = Class.forName("android.media.IAudioService$Stub").getMethod("asInterface", IBinder.class).invoke(null, service);
                    } catch (Exception e) {
                        Slog.e(TAG, "All reflection paths failed for IAudioService", e);
                        return null;
                    }
                }
            }
            if (objInvoke != null) {
                Slog.d(TAG, "Successfully obtained IAudioService interface");
                return (IInterface) objInvoke;
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
