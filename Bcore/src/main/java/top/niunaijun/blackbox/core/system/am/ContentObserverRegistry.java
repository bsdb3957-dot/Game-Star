package top.niunaijun.blackbox.core.system.am;

import android.net.Uri;
import android.os.IBinder;
import android.os.Parcel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import top.niunaijun.blackbox.utils.Slog;

/**
 * Server-side registry of guest content observers. Keeps virtual change
 * notifications inside the sandbox instead of handing guest observer binders
 * to the real framework ContentService.
 */
public class ContentObserverRegistry {
    private static final String TAG = "ContentObservers";
    private static final String DESCRIPTOR = "android.database.IContentObserver";
    private static final String STUB_CLASS = "android.database.IContentObserver$Stub";

    private static volatile Method sOnChange;

    private static final class Entry {
        final List<String> segments;
        final boolean descendants;
        final IBinder observer;
        final int userId;
        final IBinder.DeathRecipient death;

        Entry(List<String> segments, boolean descendants, IBinder observer, int userId, IBinder.DeathRecipient death) {
            this.segments = segments;
            this.descendants = descendants;
            this.observer = observer;
            this.userId = userId;
            this.death = death;
        }
    }

    private final List<Entry> mEntries = new ArrayList<>();

    private static List<String> segments(Uri uri) {
        List<String> segments = new ArrayList<>();
        segments.add(String.valueOf(uri.getAuthority()));
        segments.addAll(uri.getPathSegments());
        return segments;
    }

    private static boolean matches(Entry entry, List<String> segments) {
        List<String> registered = entry.segments;
        int common = Math.min(registered.size(), segments.size());
        for (int i = 0; i < common; i++) {
            if (!registered.get(i).equals(segments.get(i))) {
                return false;
            }
        }
        return registered.size() <= segments.size() || entry.descendants;
    }

    private static void dispatch(IBinder observer, boolean selfChange, Uri uri) {
        try {
            Object stub = Class.forName(STUB_CLASS)
                    .getMethod("asInterface", IBinder.class)
                    .invoke(null, observer);
            Method onChange = sOnChange;
            if (onChange == null) {
                try {
                    onChange = stub.getClass().getMethod("onChange", boolean.class, Uri.class, int.class);
                } catch (NoSuchMethodException e) {
                    onChange = stub.getClass().getMethod("onChange", boolean.class, Uri.class);
                }
                sOnChange = onChange;
            }
            if (onChange.getParameterCount() == 3) {
                onChange.invoke(stub, selfChange, uri, 0);
            } else {
                onChange.invoke(stub, selfChange, uri);
            }
            return;
        } catch (Throwable ignored) {
        }
        // raw binder transaction, matching the IContentObserver AIDL layout
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeInterfaceToken(DESCRIPTOR);
            parcel.writeInt(selfChange ? 1 : 0);
            parcel.writeInt(1);
            uri.writeToParcel(parcel, 0);
            parcel.writeInt(0);
            observer.transact(1, parcel, null, IBinder.FLAG_ONEWAY);
        } catch (Throwable t) {
            Slog.w(TAG, "could not notify an observer of " + uri + ": " + t);
        } finally {
            parcel.recycle();
        }
    }

    public void notifyChange(Uri uri, IBinder notifyingObserver, boolean descendants, int userId) {
        if (uri == null) {
            return;
        }
        List<String> segments = segments(uri);
        List<Entry> targets = new ArrayList<>();
        synchronized (mEntries) {
            for (Entry entry : mEntries) {
                if (entry.userId == userId
                        && (entry.observer != notifyingObserver || descendants)
                        && matches(entry, segments)) {
                    targets.add(entry);
                }
            }
        }
        for (Entry entry : targets) {
            dispatch(entry.observer, entry.observer == notifyingObserver, uri);
        }
    }

    public void register(Uri uri, boolean descendants, IBinder observer, int userId) {
        if (uri == null || observer == null) {
            return;
        }
        IBinder.DeathRecipient death = () -> unregister(observer);
        try {
            observer.linkToDeath(death, 0);
            synchronized (mEntries) {
                mEntries.add(new Entry(segments(uri), descendants, observer, userId, death));
            }
        } catch (Throwable ignored) {
        }
    }

    public void unregister(IBinder observer) {
        if (observer == null) {
            return;
        }
        synchronized (mEntries) {
            Iterator<Entry> it = mEntries.iterator();
            while (it.hasNext()) {
                Entry entry = it.next();
                if (entry.observer == observer) {
                    try {
                        observer.unlinkToDeath(entry.death, 0);
                    } catch (Throwable ignored) {
                    }
                    it.remove();
                }
            }
        }
    }
}
