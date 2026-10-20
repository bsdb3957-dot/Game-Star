package top.niunaijun.blackbox.utils;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import top.niunaijun.blackbox.utils.compat.ParceledListSliceCompat;

public class MethodReturnTypes {
    public static Object defaultValue(Class<?> cls) {
        if (cls != null && cls.isPrimitive() && cls != Void.TYPE) {
            if (cls == Boolean.TYPE) {
                return Boolean.FALSE;
            }
            if (cls == Integer.TYPE) {
                return 0;
            }
            if (cls == Long.TYPE) {
                return 0L;
            }
            if (cls == Float.TYPE) {
                return Float.valueOf(0.0f);
            }
            if (cls == Double.TYPE) {
                return Double.valueOf(0.0d);
            }
            if (cls == Short.TYPE) {
                return (short) 0;
            }
            if (cls == Byte.TYPE) {
                return (byte) 0;
            }
            if (cls == Character.TYPE) {
                return (char) 0;
            }
        }
        return null;
    }

    public static Object emptyOf(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.isPrimitive()) {
            return defaultValue(returnType);
        }
        if (List.class.isAssignableFrom(returnType) && returnType.isAssignableFrom(ArrayList.class)) {
            return new ArrayList();
        }
        if (Map.class.isAssignableFrom(returnType) && returnType.isAssignableFrom(HashMap.class)) {
            return new HashMap();
        }
        if (Set.class.isAssignableFrom(returnType) && returnType.isAssignableFrom(HashSet.class)) {
            return new HashSet();
        }
        if (returnType.isArray()) {
            return Array.newInstance(returnType.getComponentType(), 0);
        }
        if (returnType == Bundle.class) {
            return new Bundle();
        }
        if (returnType == IBinder.class) {
            return new Binder();
        }
        if ("android.content.pm.ParceledListSlice".equals(returnType.getName())) {
            return ParceledListSliceCompat.create(new ArrayList());
        }
        try {
            Constructor<?> declaredConstructor = returnType.getDeclaredConstructor(List.class);
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance(new ArrayList());
        } catch (Throwable unused) {
            return null;
        }
    }

    public static boolean isUnboxableNull(Method method, Object who) {
        if (who == null && method != null) {
            Class<?> returnType = method.getReturnType();
            if (returnType.isPrimitive() && returnType != Void.TYPE) {
                return true;
            }
        }
        return false;
    }

    public static Object defaultValue(Method method) {
        if (method == null) {
            return null;
        }
        return defaultValue(method.getReturnType());
    }
}
