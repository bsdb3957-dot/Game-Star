package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.ServiceManager;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.MethodReturnTypes;
import top.niunaijun.blackbox.utils.Slog;

public class SystemServiceGuard implements IInjectHook {
    private static final String TAG = "SystemServiceGuard";
    private static final Set<String> sLogged = Collections.newSetFromMap(new ConcurrentHashMap());
    private static final Set<String> CALLBACK_REFUSALS = Collections.singleton("app_function.setAppFunctionEnabled");
    private static final Set<String> SKIP = new HashSet(Arrays.asList("activity", "activity_task", "package", "package_native", "permissionmgr", "permission", "window", "input", "input_method", "display", "user", "appops", "uimode", "power"));

    public static final class LazyCache extends AbstractMap<String, IBinder> {
        private static final ThreadLocal<Boolean> sResolving = new ThreadLocal<>();
        private final Method mCheckService;
        private final ConcurrentHashMap<String, IBinder> mServices = new ConcurrentHashMap<>();
        private final Set<String> mPlain = Collections.newSetFromMap(new ConcurrentHashMap());

        private LazyCache(Map<String, IBinder> map, Method method) {
            for (Map.Entry<String, IBinder> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    this.mServices.put(entry.getKey(), entry.getValue());
                }
            }
            this.mCheckService = method;
        }

        public static boolean install() {
            try {
                Field declaredField = ServiceManager.class.getDeclaredField("sCache");
                declaredField.setAccessible(true);
                Method declaredMethod = ServiceManager.class.getDeclaredMethod("checkService", String.class);
                declaredMethod.setAccessible(true);
                Map map = (Map) declaredField.get(null);
                if (map instanceof LazyCache) {
                    return true;
                }
                if (map == null) {
                    map = Collections.EMPTY_MAP;
                }
                declaredField.set(null, new LazyCache(map, declaredMethod));
                return declaredField.get(null) instanceof LazyCache;
            } catch (Throwable th) {
                Slog.w(SystemServiceGuard.TAG, "cannot guard platform services lazily: " + th);
                return false;
            }
        }

        @Override
        public boolean containsKey(Object who) {
            return who != null && this.mServices.containsKey(who);
        }

        @Override
        public Set<Map.Entry<String, IBinder>> entrySet() {
            return this.mServices.entrySet();
        }

        @Override
        public int size() {
            return this.mServices.size();
        }

        @Override
        public IBinder get(Object who) {
            IBinder iBinder = this.mServices.get(who);
            if (iBinder != null || !(who instanceof String)) {
                return iBinder;
            }
            String str = (String) who;
            if (!SystemServiceGuard.SKIP.contains(str) && !this.mPlain.contains(str)) {
                ThreadLocal<Boolean> threadLocal = sResolving;
                if (threadLocal.get() == null) {
                    threadLocal.set(Boolean.TRUE);
                    try {
                        IBinder iBinder2 = (IBinder) this.mCheckService.invoke(null, str);
                        threadLocal.remove();
                        if (iBinder2 != null) {
                            IBinder iBinderC = SystemServiceGuard.wrap(str, iBinder2);
                            if (iBinderC == null) {
                                this.mPlain.add(str);
                                return iBinder2;
                            }
                            IBinder iBinderPutIfAbsent = this.mServices.putIfAbsent(str, iBinderC);
                            return iBinderPutIfAbsent != null ? iBinderPutIfAbsent : iBinderC;
                        }
                    } catch (Throwable unused) {
                        sResolving.remove();
                    }
                }
            }
            return null;
        }

        @Override
        public IBinder put(String str, IBinder iBinder) {
            if (str == null) {
                return null;
            }
            return iBinder == null ? this.mServices.remove(str) : this.mServices.put(str, iBinder);
        }

        @Override
        public IBinder remove(Object who) {
            if (who == null) {
                return null;
            }
            return this.mServices.remove(who);
        }
    }

        private static IBinder guard(final String serviceName, String descriptor, Class<?> cls, final Object who) {
        final Binder binder = new Binder();
        binder.attachInterface((IInterface) Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxyInvocation, Method method, Object[] args) throws Throwable {
                return guardInvocation(binder, serviceName, who, proxyInvocation, method, args);
            }
        }), descriptor);
        return binder;
    }
    public static Object lambda$guard$0(Binder binder, String str, Object who, Object proxyInvocation, Method method, Object[] args) throws Throwable {
        if ("asBinder".equals(method.getName()) && (args == null || args.length == 0)) {
            return binder;
        }
        if (CALLBACK_REFUSALS.contains(serviceName + "." + method.getName())) {
            MethodParameterUtils.replaceFirstAppPkg(args);
        }
        try {
            return method.invoke(who, args);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (!(targetException instanceof SecurityException)) {
                throw targetException;
            }
            if (args != null) {
                Object[] retryArgs = (Object[]) args.clone();
                if (MethodParameterUtils.replaceFirstAppPkg(retryArgs) != null) {
                    try {
                        return method.invoke(who, retryArgs);
                    } catch (InvocationTargetException retryException) {
                        if (!(retryException.getTargetException() instanceof SecurityException)) {
                            throw retryException.getTargetException();
                        }
                        if (sLogged.add(serviceName + "." + method.getName())) {
                            Slog.w(TAG, serviceName + "." + method.getName() + " refused for a guest, answering empty: " + targetException.getMessage());
                        }
                        return MethodReturnTypes.emptyOf(method);
                    }
                }
            }
            if (sLogged.add(serviceName + "." + method.getName())) {
                Slog.w(TAG, serviceName + "." + method.getName() + " refused for a guest, answering empty: " + targetException.getMessage());
            }
            return MethodReturnTypes.emptyOf(method);
        }
    }

    private static IBinder wrap(String str, IBinder iBinder) {
        Object objInvoke;
        try {
            String interfaceDescriptor = iBinder.getInterfaceDescriptor();
            if (interfaceDescriptor != null && !interfaceDescriptor.isEmpty()) {
                Class<?> cls = Class.forName(interfaceDescriptor, false, Binder.class.getClassLoader());
                if (cls.isInterface() && (objInvoke = Class.forName(interfaceDescriptor.concat("$Stub"), false, Binder.class.getClassLoader()).getMethod("asInterface", IBinder.class).invoke(null, iBinder)) != null) {
                    return guard(str, interfaceDescriptor, cls, objInvoke);
                }
                return null;
            }
        } catch (Throwable unused) {
        }
        return null;
    }

    @Override
    public void injectHook() {
        if (BlackBoxCore.get().isBlackProcess()) {
            if (LazyCache.install()) {
                Slog.d(TAG, "guarding platform services on first use");
                return;
            }
            try {
                String[] strArrListServices = BRServiceManager.get().listServices();
                if (strArrListServices == null) {
                    return;
                }
                Map<String, IBinder> mapSCache = BRServiceManager.get().sCache();
                int i = 0;
                for (String str : strArrListServices) {
                    if (str != null && !mapSCache.containsKey(str) && !SKIP.contains(str)) {
                        try {
                            IBinder service = BRServiceManager.get().getService(str);
                            IBinder iBinderWrap = service == null ? null : wrap(str, service);
                            if (iBinderWrap != null) {
                                mapSCache.put(str, iBinderWrap);
                                i++;
                            }
                        } catch (Throwable unused) {
                        }
                    }
                }
                Slog.d(TAG, "guarding " + i + " platform services");
            } catch (Throwable unused2) {
            }
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
