package singletons;

public class CloneableSingleton implements Cloneable {
    private static CloneableSingleton instance;

    private CloneableSingleton() {
    }

    public static synchronized CloneableSingleton getInstance() {
      if (instance == null) {
        instance = new CloneableSingleton();
      }
      return instance;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
