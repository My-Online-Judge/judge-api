package vn.thanhtuanle.common.util;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;

public class PropertyUtil {

    @FunctionalInterface
    public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
    }

    public static <T, R> String name(SerializableFunction<T, R> getter) {
        try {
            Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(getter);

            String methodName = serializedLambda.getImplMethodName();

            if (methodName.startsWith("get")) {
                return lowerFirst(methodName.substring(3));
            } else if (methodName.startsWith("is")) {
                return lowerFirst(methodName.substring(2));
            }
            return methodName;
        } catch (Exception e) {
            throw new RuntimeException("Could not resolve property name", e);
        }
    }

    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private PropertyUtil() {
    }
}
