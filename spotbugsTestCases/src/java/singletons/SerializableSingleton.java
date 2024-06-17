package singletons;

import java.io.Serializable;

class SerializableSingleton implements Serializable {
    private static SerializableSingleton instance;
   
    private SerializableSingleton() {
      // Private constructor prevents
      // instantiation by untrusted callers
    }
   
    // Lazy initialization
    public static synchronized SerializableSingleton getInstance() {
      if (instance == null) {
        instance = new SerializableSingleton();
      }
      return instance;
    }
}
