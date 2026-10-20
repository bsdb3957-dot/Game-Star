package black.android.content;

import android.os.IBinder;
import android.os.IInterface;
import java.lang.reflect.Method;
import top.niunaijun.blackreflection.annotation.BClassNameNotProcess;
import top.niunaijun.blackreflection.annotation.BMethodCheckNotProcess;

/* JADX INFO: compiled from: SRC */
/* JADX INFO: loaded from: classes.dex */
@BClassNameNotProcess("android.content.IClipboard$Stub")
public interface IClipboardStubStatic {
    @BMethodCheckNotProcess
    Method _check_asInterface(IBinder iBinder);

    IInterface asInterface(IBinder iBinder);
}
