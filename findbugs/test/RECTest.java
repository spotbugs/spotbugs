
import java.io.IOException;

/**
 * RECTest
 *
 * @author Brian Goetz
 */
public class RECTest {
	public static Exception anException = new IOException();
	
	public static void staticThrowsException() throws Exception { throw new Exception(); }
	public static void staticThrowsIOException() throws IOException { throw new IOException(); }

	public void throwsException() throws Exception { throw new Exception(); }
	public void throwsIOException() throws IOException { throw new IOException(); }

	private void dontTriggerEmptyExceptionHandler() { }

	// should fail -- catches E, but E not thrown
	public void testFail() {
		try {
			int i=0;
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail2() {
		try {
			throw new IOException();
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail3() {
		try {
			IOException e = new IOException();
			throw e;
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail4() {
		try {
			throwsIOException();
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail5() {
		try {
			staticThrowsIOException();
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown
	public void testPass() {
		try {
			throw new Exception();
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown indirectly
	public void testPass2() {
		try {
			throw anException;
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown by method
	public void testPass3() {
		try {
			throwsException();
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown by static method
	public void testPass4() {
		try {
			staticThrowsException();
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but RuntimeException caught first
	public void testPass5() {
		try {
			throwsIOException();
		}
		catch (RuntimeException e) {
			dontTriggerEmptyExceptionHandler();
		}
		catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}
}
