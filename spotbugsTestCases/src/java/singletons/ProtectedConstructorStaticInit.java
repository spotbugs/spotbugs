package singletons;

class ProtectedConstructorStaticInit {
    protected ProtectedConstructorStaticInit() {
        
    }

    public static synchronized ProtectedConstructorStaticInit getInstance() {
      return instance;
    }
    
    private static ProtectedConstructorStaticInit instance;

    static {
        instance = new ProtectedConstructorStaticInit();
    }
}
