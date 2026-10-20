package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class AudioRecordProxy extends ClassInvocationStub {
    public static final String TAG = "AudioRecordProxy";

    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(AudioRecordProxy.TAG, "AudioRecord: Constructor called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getState")
    public static class GetState extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(AudioRecordProxy.TAG, "AudioRecord: getState called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("read")
    public static class Read extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(AudioRecordProxy.TAG, "AudioRecord: read called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("release")
    public static class Release extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(AudioRecordProxy.TAG, "AudioRecord: release called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("startRecording")
    public static class StartRecording extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(AudioRecordProxy.TAG, "AudioRecord: startRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("stop")
    public static class Stop extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(AudioRecordProxy.TAG, "AudioRecord: stop called, allowing");
            return method.invoke(who, args);
        }
    }

    @Override
    public Object getWho() {
        return null;
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
    }
}
