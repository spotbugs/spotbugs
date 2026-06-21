package singletons;

public class MultipleNonPrivateConstructors {
    private static MultipleNonPrivateConstructors instance;

    private MultipleNonPrivateConstructors() {
    }

    protected MultipleNonPrivateConstructors(Object parameter) {
    }

    public static synchronized MultipleNonPrivateConstructors getInstance() {   
        if (instance == null) {
            instance = new MultipleNonPrivateConstructors();
        }
        return instance;
    }
}
