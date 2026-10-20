package top.niunaijun.blackbox.fake.service;

import java.io.File;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class FileSystemProxy extends ClassInvocationStub {
    public static final String TAG = "FileSystemProxy";

    @ProxyMethod("isDirectory")
    public static class IsDirectory extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                String absolutePath = ((File) who).getAbsolutePath();
                if (!absolutePath.contains("Helium Crashpad") && !absolutePath.contains("HeliumCrashReporter")) {
                    return method.invoke(who, args);
                }
                Slog.d(FileSystemProxy.TAG, "FileSystem: isDirectory called for Helium crash path: " + absolutePath + ", returning true");
                return Boolean.TRUE;
            } catch (Exception e) {
                Slog.w(FileSystemProxy.TAG, "FileSystem: isDirectory failed, returning false", e);
                return Boolean.FALSE;
            }
        }
    }

    @ProxyMethod("mkdir")
    public static class Mkdir extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                String absolutePath = ((File) who).getAbsolutePath();
                if (!absolutePath.contains("Helium Crashpad") && !absolutePath.contains("HeliumCrashReporter")) {
                    return method.invoke(who, args);
                }
                Slog.d(FileSystemProxy.TAG, "FileSystem: mkdir called for Helium crash path: " + absolutePath + ", returning true");
                return Boolean.TRUE;
            } catch (Exception e) {
                Slog.w(FileSystemProxy.TAG, "FileSystem: mkdir failed, returning true", e);
                return Boolean.TRUE;
            }
        }
    }

    @ProxyMethod("mkdirs")
    public static class Mkdirs extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                String absolutePath = ((File) who).getAbsolutePath();
                if (!absolutePath.contains("Helium Crashpad") && !absolutePath.contains("HeliumCrashReporter")) {
                    return method.invoke(who, args);
                }
                Slog.d(FileSystemProxy.TAG, "FileSystem: mkdirs called for Helium crash path: " + absolutePath + ", returning true");
                return Boolean.TRUE;
            } catch (Exception e) {
                Slog.w(FileSystemProxy.TAG, "FileSystem: mkdirs failed, returning true", e);
                return Boolean.TRUE;
            }
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
