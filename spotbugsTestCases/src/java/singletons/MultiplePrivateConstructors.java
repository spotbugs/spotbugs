package singletons;

public class MultiplePrivateConstructors {
    private static MultiplePrivateConstructors instance;

    private MultiplePrivateConstructors() {
    }

    private MultiplePrivateConstructors(Object parameter) {
    }

    public static synchronized MultiplePrivateConstructors getInstance() {   
        if (instance == null) {
            instance = new MultiplePrivateConstructors();
        }
        return instance;
    }
}
