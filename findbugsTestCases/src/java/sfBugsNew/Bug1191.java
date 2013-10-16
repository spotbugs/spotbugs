package sfBugsNew;

public class Bug1191 {
    public void doSomething() {}

    public class TestInnerClass {
      public void doSomethingInnerClass() {
        new Runnable() {
          @Override
            public void run() {
              doSomething();
            }
          }.run();
      }
    }
}
