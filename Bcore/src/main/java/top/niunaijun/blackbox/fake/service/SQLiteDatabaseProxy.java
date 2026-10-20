package top.niunaijun.blackbox.fake.service;

import android.database.sqlite.SQLiteException;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class SQLiteDatabaseProxy extends ClassInvocationStub {
    public static final String TAG = "SQLiteDatabaseProxy";

    @ProxyMethod("execSQL")
    public static class ExecSQL extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                String str = (String) args[0];
                if (str == null || !str.contains("DurableJob")) {
                    return method.invoke(who, args);
                }
                Slog.w(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: execSQL called with DurableJob table, ignoring");
                return null;
            } catch (SQLiteException e) {
                Slog.w(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: execSQL failed, ignoring", e);
                return null;
            } catch (Exception e) {
                Slog.e(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: execSQL error", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("query")
    public static class Query extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                String str = (String) args[0];
                if (str == null || !str.equals("DurableJob")) {
                    return method.invoke(who, args);
                }
                Slog.w(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: query called on DurableJob table, returning empty cursor");
                return null;
            } catch (SQLiteException e) {
                Slog.w(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: query failed, returning null", e);
                return null;
            } catch (Exception e) {
                Slog.e(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: query error", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("rawQuery")
    public static class RawQuery extends MethodHook {
        @Override
        public Object hook(Object who, Method method, Object[] args) {
            try {
                String str = (String) args[0];
                if (str == null || !str.contains("DurableJob")) {
                    return method.invoke(who, args);
                }
                Slog.w(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: rawQuery called with DurableJob table, returning empty cursor");
                return null;
            } catch (SQLiteException e) {
                Slog.w(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: rawQuery failed, returning null", e);
                return null;
            } catch (Exception e) {
                Slog.e(SQLiteDatabaseProxy.TAG, "SQLiteDatabase: rawQuery error", e);
                return method.invoke(who, args);
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
