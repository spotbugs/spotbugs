package singletons;

class ProtectedConstructor {   
    protected ProtectedConstructor() {
    }

    public static synchronized ProtectedConstructor getInstance() {   
        if (instance == null) {
            instance = new ProtectedConstructor();
        }
      return instance;
    }
    
    private static ProtectedConstructor instance;
}
