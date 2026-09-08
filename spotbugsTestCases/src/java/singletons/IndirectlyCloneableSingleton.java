package singletons;

class IndirectlyCloneableSingleton extends CloneableClass {
    private static IndirectlyCloneableSingleton instance;

    private IndirectlyCloneableSingleton() {
      // Private constructor prevents
      // instantiation by untrusted callers
    }

    // Lazy initialization
    public static synchronized IndirectlyCloneableSingleton getInstance() {
      if (instance == null) {
        instance = new IndirectlyCloneableSingleton();
      }
      return instance;
    }
}
