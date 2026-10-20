package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.IInterface;
import android.os.Parcel;
import black.android.net.ITetheringConnectorStubStatic;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackreflection.BlackReflection;

public class ITetheringConnectorProxy extends BinderInvocationStub {
    private static final String LISTENER = "android.net.IIntResultListener";
    private static final String SERVICE = "tethering";
    private static final int TETHER_ERROR_NO_ACCESS_TETHERING_PERMISSION = 15;
    private static final int TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION = 14;
    private static final int TETHER_ERROR_UNSUPPORTED = 3;

    public static final class ResultRelay extends Binder {
        private final IBinder mTarget;

        public ResultRelay(IBinder iBinder) {
            this.mTarget = iBinder;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 1) {
                return super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface(ITetheringConnectorProxy.LISTENER);
            int errorCode = data.readInt();
            if (errorCode == ITetheringConnectorProxy.TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION || errorCode == ITetheringConnectorProxy.TETHER_ERROR_NO_ACCESS_TETHERING_PERMISSION) {
                errorCode = 3;
            }
            Parcel obtain = Parcel.obtain();
            try {
                obtain.writeInterfaceToken(ITetheringConnectorProxy.LISTENER);
                obtain.writeInt(errorCode);
                this.mTarget.transact(1, obtain, null, 1);
                return true;
            } finally {
                obtain.recycle();
            }
        }
    }

    public ITetheringConnectorProxy() {
        super(BRServiceManager.get().getService(SERVICE));
    }

    private static Object relayed(IInterface iInterface, Class<?> cls) {
        try {
            IBinder iBinderAsBinder = iInterface.asBinder();
            if (iBinderAsBinder != null && LISTENER.equals(iBinderAsBinder.getInterfaceDescriptor())) {
                return Class.forName("android.net.IIntResultListener$Stub", false, cls.getClassLoader()).getMethod("asInterface", IBinder.class).invoke(null, new ResultRelay(iBinderAsBinder));
            }
            return iInterface;
        } catch (Throwable unused) {
            return iInterface;
        }
    }

    @Override
    public Object getWho() {
        return ((ITetheringConnectorStubStatic) BlackReflection.create(ITetheringConnectorStubStatic.class, null, false)).asInterface(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    public void inject(Object who, Object proxyInvocation) {
        replaceSystemService(SERVICE);
    }

    @Override
    public Object invoke(Object who, Method method, Object[] args) throws Throwable {
        if (args != null) {
            MethodParameterUtils.replaceFirstAppPkg(args);
            for (int i = 0; i < args.length; i++) {
                Object proxyInvocation = args[i];
                if (proxyInvocation instanceof IInterface) {
                    args[i] = relayed((IInterface) proxyInvocation, method.getParameterTypes()[i]);
                }
            }
        }
        return super.invoke(who, method, args);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
