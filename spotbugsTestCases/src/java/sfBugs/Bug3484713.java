package sfBugs;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.annotations.NoWarning;


public class Bug3484713 {
  @CheckForNull
  private String foo;

  @NoWarning("NP")
  public Runnable doFoo() {
    if (foo != null) {
      // No warning here.
      System.out.println(foo.equals("foo"));
    }
    Runnable r = new Runnable() {
      @NoWarning("NP")
      @Override
      public void run() {
        if (foo != null) {
          /*
           * Getting a findbugs warning on next line:
           * "Possible null pointer dereference in WrongNPWarning$1.run() due to return value of called method"
           */
          System.out.println(foo.equals("foo"));
        }
      }
    };
    return r;
  }
}
