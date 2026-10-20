package black.android.safetycenter;

import android.os.IBinder;
import android.os.IInterface;
import java.lang.reflect.Method;
import top.niunaijun.blackreflection.annotation.BClassNameNotProcess;
import top.niunaijun.blackreflection.annotation.BMethodCheckNotProcess;

/* JADX INFO: compiled from: SRC */
/* JADX INFO: loaded from: classes.dex */
@BClassNameNotProcess("android.safetycenter.ISafetyCenterManager$Stub")
public interface ISafetyCenterManagerStubStatic {
    @BMethodCheckNotProcess
    Method _check_asInterface(IBinder iBinder);

    IInterface asInterface(IBinder iBinder);
}
