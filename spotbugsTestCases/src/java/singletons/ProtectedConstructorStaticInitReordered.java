package singletons;

class ProtectedConstructorStaticInitReordered {
    protected ProtectedConstructorStaticInitReordered() {
        
    }

    static {
        instance = new ProtectedConstructorStaticInitReordered();
    }

    public static synchronized ProtectedConstructorStaticInitReordered getInstance() {
      return instance;
    }
    
    private static ProtectedConstructorStaticInitReordered instance;

}
