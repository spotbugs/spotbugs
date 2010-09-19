package sfBugs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Bug1917352 implements Serializable {
    private static final long serialVersionUID = 1L;

    private Class javaClass;

    private Constructor[] constructors = new Constructor[10];

    private Method[] methods = new Method[10];

    public static final boolean useContextClassLoader = false;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(javaClass.getName());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        javaClass = getClassObject(in.readUTF());
        constructors = javaClass.getConstructors();
        methods = null;
    }

    private Class getClassObject(String name) throws ClassNotFoundException {
        if (useContextClassLoader)
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        else
            return Class.forName(name);
    }

    public Constructor getConstructor(int i) {
        return constructors[i];
    }

    public Method getMethod(int i) {
        return methods[i];
    }

    public void setConstructor(int i, Constructor c) {
        constructors[i] = c;
    }

    public void setMethod(int i, Method m) {
        methods[i] = m;
    }
}
