package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1191 {
    public void doSomething() {}

    public class TestInnerClass {
      public void doSomethingInnerClass() {
        new Runnable() {
          @NoWarning("IMA_INEFFICIENT_MEMBER_ACCESS")
          @Override
            public void run() {
              doSomething();
            }
          }.run();
      }
    }
}
