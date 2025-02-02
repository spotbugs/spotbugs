package singletons;

public class MultipleNonPrivateConstructors2 {
    private static MultipleNonPrivateConstructors2 instance;

    public MultipleNonPrivateConstructors2(Object parameter) {
    }

    private MultipleNonPrivateConstructors2() {
    }

    public static synchronized MultipleNonPrivateConstructors2 getInstance() {   
        if (instance == null) {
            instance = new MultipleNonPrivateConstructors2();
        }
        return instance;
    }
}
