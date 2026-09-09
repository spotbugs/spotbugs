package singletons;

class AppropriateSingleton {   
    private static AppropriateSingleton instance;

    private AppropriateSingleton() {
    }

    public static synchronized AppropriateSingleton getInstance() {   
        if (instance == null) {
            instance = new AppropriateSingleton();
        }
        return instance;
    }
}
