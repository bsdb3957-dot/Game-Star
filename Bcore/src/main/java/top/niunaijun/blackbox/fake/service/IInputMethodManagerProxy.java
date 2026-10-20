package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import o5.g;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.hook.ScanClass;

@ScanClass({IInputMethodManagerProxy.class})
public class IInputMethodManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IInputMethodManagerProxy";

    @ProxyMethod("startInputOrWindowGainedFocus")
    public static class StartInputOrWindowGainedFocus extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            return method.invoke(who, args);
        }
    }

    public IInputMethodManagerProxy() {
        super(g.a().getService("input_method"));
    }

    @Override
    public Object getWho() {
        return g.a().getService("input_method");
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService("input_method");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
