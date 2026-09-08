package singletons;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3727">GitHub issue #3727</a>
  */
public class CloneableSingletonWithoutCloneMethod implements Cloneable {
    private static CloneableSingletonWithoutCloneMethod instance;

    private CloneableSingletonWithoutCloneMethod() {
    }

    public static synchronized CloneableSingletonWithoutCloneMethod getInstance() {
      if (instance == null) {
        instance = new CloneableSingletonWithoutCloneMethod();
      }
      return instance;
    }
}
