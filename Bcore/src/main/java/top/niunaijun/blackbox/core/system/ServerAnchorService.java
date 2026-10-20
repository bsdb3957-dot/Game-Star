package top.niunaijun.blackbox.core.system;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Empty service hosted in the server process; a sticky binding to it from a
 * guest process keeps the server alive. See BActivityThread.anchorServer().
 */
public class ServerAnchorService extends Service {
    private final IBinder mBinder = new Binder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
