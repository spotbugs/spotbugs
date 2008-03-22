package concurrency;


public class NonFinalClassShouldUseClassLiteral {
	  private static int count;
	  public NonFinalClassShouldUseClassLiteral() {
	    synchronized (getClass()) {
	      count++;
	    }
	  }
	}
