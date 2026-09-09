package singletons;

class NotSynchronizedSingleton {
    private static NotSynchronizedSingleton instance;
   
    private NotSynchronizedSingleton() {
    }

    public static NotSynchronizedSingleton getInstance() { // Not synchronized
      if (instance == null) {
        instance = new NotSynchronizedSingleton();
      }
      return instance;
    }
}
