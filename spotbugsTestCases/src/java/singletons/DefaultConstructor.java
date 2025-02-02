package singletons;

class DefaultConstructor {
    private static DefaultConstructor instance;

    // Lazy initialization
    public static synchronized DefaultConstructor getInstance() {
      if (instance == null) {
        instance = new DefaultConstructor();
      }
      return instance;
    }
}
