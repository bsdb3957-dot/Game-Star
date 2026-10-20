package top.niunaijun.blackbox.fake.service;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import black.android.app.IServiceConnectionOContext;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.utils.GuestIds;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

public final class GmsIdSpoof {
    private static final String AD_ID_ACTION = "com.google.android.gms.ads.identifier.service.START";
    private static final String AD_ID_DESCRIPTOR = "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService";
    private static final String APPSET_ACTION = "com.google.android.gms.appset.service.START";
    private static final String APPSET_CALLBACK_DESC = "com.google.android.gms.appset.internal.IAppSetIdCallback";
    private static final String APPSET_SERVICE_DESC = "com.google.android.gms.appset.internal.IAppSetService";
    private static final int OBJECT_MAGIC = -45243;
    private static final String TAG = "GmsIdSpoof";

    private GmsIdSpoof() {
    }

    private static int beginObject(Parcel parcel) {
        parcel.writeInt(OBJECT_MAGIC);
        parcel.writeInt(0);
        return parcel.dataPosition();
    }

    public static boolean deliver(Intent intent, IServiceConnection iServiceConnection) {
        final String strGaid = GuestIds.gaid();
        if (strGaid != null && iServiceConnection != null) {
            try {
                Binder binder = new Binder() { // from class: top.niunaijun.blackbox.fake.service.GmsIdSpoof.1
                    @Override
                    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i) {
                        if (i == 1) {
                            parcel.enforceInterface(GmsIdSpoof.AD_ID_DESCRIPTOR);
                            if (parcel2 != null) {
                                parcel2.writeNoException();
                                parcel2.writeString(strGaid);
                            }
                            return true;
                        }
                        if (i != 2) {
                            if (i != 1598968902) {
                                return super.onTransact(i, parcel, parcel2, i);
                            }
                            if (parcel2 != null) {
                                parcel2.writeString(GmsIdSpoof.AD_ID_DESCRIPTOR);
                            }
                            return true;
                        }
                        parcel.enforceInterface(GmsIdSpoof.AD_ID_DESCRIPTOR);
                        if (parcel2 != null) {
                            parcel2.writeNoException();
                            parcel2.writeInt(0);
                        }
                        return true;
                    }
                };
                ComponentName component = intent.getComponent() != null ? intent.getComponent() : new ComponentName(GmsCore.GMS_PKG, "com.google.android.gms.ads.identifier.service.AdvertisingIdService");
                if (BuildCompat.isOreo()) {
                    ((IServiceConnectionOContext) BlackReflection.create(IServiceConnectionOContext.class, iServiceConnection, false)).connected(component, binder, false);
                } else {
                    iServiceConnection.connected(component, binder);
                }
                Slog.d(TAG, "served spoofed GAID ".concat(strGaid));
                return true;
            } catch (Throwable th) {
                Slog.w(TAG, "GAID spoof failed, falling back to real bind: " + th);
            }
        }
        return false;
    }

    public static boolean deliverAppSet(Intent intent, IServiceConnection iServiceConnection) {
        final String strAppSetId = GuestIds.appSetId();
        if (strAppSetId != null && iServiceConnection != null) {
            try {
                Binder binder = new Binder() { // from class: top.niunaijun.blackbox.fake.service.GmsIdSpoof.2
                    @Override
                    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i) {
                        if (i != 1) {
                            if (i != 1598968902) {
                                return super.onTransact(i, parcel, parcel2, i);
                            }
                            if (parcel2 != null) {
                                parcel2.writeString(GmsIdSpoof.APPSET_SERVICE_DESC);
                            }
                            return true;
                        }
                        parcel.enforceInterface(GmsIdSpoof.APPSET_SERVICE_DESC);
                        if (parcel.readInt() != 0) {
                            GmsIdSpoof.skipSafeParcelable(parcel);
                        }
                        IBinder strongBinder = parcel.readStrongBinder();
                        if (parcel2 != null) {
                            parcel2.writeNoException();
                        }
                        GmsIdSpoof.deliverAppSetResult(strongBinder, strAppSetId);
                        return true;
                    }
                };
                ComponentName component = intent.getComponent() != null ? intent.getComponent() : new ComponentName(GmsCore.GMS_PKG, "com.google.android.gms.appset.service.AppSetIdProviderService");
                if (BuildCompat.isOreo()) {
                    ((IServiceConnectionOContext) BlackReflection.create(IServiceConnectionOContext.class, iServiceConnection, false)).connected(component, binder, false);
                } else {
                    iServiceConnection.connected(component, binder);
                }
                Slog.d(TAG, "served spoofed app_set_id ".concat(strAppSetId));
                return true;
            } catch (Throwable th) {
                Slog.w(TAG, "app_set_id spoof failed, falling back to real bind: " + th);
            }
        }
        return false;
    }

    public static void deliverAppSetResult(IBinder iBinder, String str) {
        if (iBinder == null) {
            return;
        }
        Parcel parcelObtain = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(APPSET_CALLBACK_DESC);
            parcelObtain.writeInt(1);
            writeStatusSuccess(parcelObtain);
            parcelObtain.writeInt(1);
            writeAppSetIdInfoParcel(parcelObtain, str, 1);
            iBinder.transact(1, parcelObtain, null, 1);
            parcelObtain.recycle();
        } catch (Throwable th) {
            try {
                Slog.w(TAG, "deliverAppSetResult failed: " + th);
            } finally {
                parcelObtain.recycle();
            }
        }
    }

    private static void finishObject(Parcel parcel, int i) {
        int iDataPosition = parcel.dataPosition();
        parcel.setDataPosition(i - 4);
        parcel.writeInt(iDataPosition - i);
        parcel.setDataPosition(iDataPosition);
    }

    public static boolean isAdIdService(String str) {
        return AD_ID_ACTION.equals(str);
    }

    public static boolean isAppSetService(String str) {
        return APPSET_ACTION.equals(str);
    }

    public static void skipSafeParcelable(Parcel parcel) {
        parcel.readInt();
        parcel.setDataPosition(parcel.dataPosition() + parcel.readInt());
    }

    private static void writeAppSetIdInfoParcel(Parcel parcel, String str, int i) {
        int iBeginObject = beginObject(parcel);
        writeStringField(parcel, 1, str);
        writeIntField(parcel, 2, i);
        finishObject(parcel, iBeginObject);
    }

    private static void writeIntField(Parcel parcel, int i, int i) {
        parcel.writeInt(i | 262144);
        parcel.writeInt(i);
    }

    private static void writeStatusSuccess(Parcel parcel) {
        int iBeginObject = beginObject(parcel);
        writeIntField(parcel, 1, 0);
        finishObject(parcel, iBeginObject);
    }

    private static void writeStringField(Parcel parcel, int i, String str) {
        parcel.writeInt(i | (-65536));
        int iDataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        int iDataPosition2 = parcel.dataPosition();
        parcel.writeString(str);
        int iDataPosition3 = parcel.dataPosition();
        parcel.setDataPosition(iDataPosition);
        parcel.writeInt(iDataPosition3 - iDataPosition2);
        parcel.setDataPosition(iDataPosition3);
    }
}
