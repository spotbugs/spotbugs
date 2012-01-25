package sfBugs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3479080 {
    @DesireNoWarning("NP_GUARANTEED_DEREF_ON_EXCEPTION_PATH")
    public Object doit()
    {
        Object targetClass = "java.lang.String";

        Class c = null;

        try
        {
            // Allow the class to be specified as a String instead of
            // passed-in as a java.lang.Class object.
            if(targetClass instanceof String)
                c = Class.forName((String)targetClass);
            else if(targetClass instanceof Class)
                c = (Class)targetClass;
            else
                throw new RuntimeException("Can't create class from " + targetClass);

            Constructor constructor = getConstructor(c);

            if(null == constructor)
                throw new RuntimeException("No such constructor found in class " + c.getName());

            return constructor.newInstance((Object[])null);
        }
        catch (IllegalAccessException iae)
        {
            throw new RuntimeException("Cannot create object of type " + c, iae);
        }
        catch (InvocationTargetException ite)
        {
            throw new RuntimeException(c.getName() + ".<init> failed", ite.getCause());
        }
        catch (InstantiationException ie)
        {
            throw new RuntimeException("Cannot create " + c.getName(), ie);
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new RuntimeException("Cannot create " + targetClass, cnfe);
        }
    }

    public Constructor getConstructor(Class c) { return null; }

}
