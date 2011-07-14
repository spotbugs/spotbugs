package tomcat;

import java.lang.reflect.Method;

public class IntrospectionUtils {
    public static Object callMethod1(Object target, String methodN, Object param1, String typeParam1, ClassLoader cl)
            throws Exception {
        if (target == null || param1 == null)
            d("Assert: Illegal params " + target + " " + param1);
        Class params[] = new Class[1];
        if (typeParam1 == null)
            params[0] = param1.getClass();
        else
            params[0] = cl.loadClass(typeParam1);
        Method m = findMethod(target.getClass(), methodN, params);
        if (m == null)
            throw new NoSuchMethodException(target.getClass().getName() + " " + methodN);
        else
            return m.invoke(target, new Object[] { param1 });
    }

    static void d(String s) {
        System.out.println("IntrospectionUtils: " + s);
    }

    public static Method findMethod(Class c, String name, Class params[]) throws SecurityException, NoSuchMethodException {
        // STUB Method; what happens here isn't relevant
        return IntrospectionUtils.class.getMethod("d", new Class[] { String.class });
    }

}
