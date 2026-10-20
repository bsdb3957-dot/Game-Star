package top.niunaijun.blackbox.fake.service;

import black.android.os.BRServiceManager;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import black.android.net.ITetheringConnectorStubStatic;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.utils.MethodParameterUtils;

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
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i) {
            if (i != 1) {
                return super.onTransact(i, parcel, parcel2, i);
            }
            parcel.enforceInterface(ITetheringConnectorProxy.LISTENER);
            int i = parcel.readInt();
            if (i == ITetheringConnectorProxy.TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION || i == ITetheringConnectorProxy.TETHER_ERROR_NO_ACCESS_TETHERING_PERMISSION) {
                i = 3;
            }
            Parcel parcelObtain = Parcel.obtain();
            try {
                parcelObtain.writeInterfaceToken(ITetheringConnectorProxy.LISTENER);
                parcelObtain.writeInt(i);
                this.mTarget.transact(1, parcelObtain, null, 1);
                return true;
            } finally {
                parcelObtain.recycle();
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
    public Object invoke(Object who, Method method, Object[] args) {
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
