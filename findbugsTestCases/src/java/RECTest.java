import java.io.IOException;

/**
 * RECTest
 * 
 * @author Brian Goetz
 */
public class RECTest {
	public static Exception anException = new IOException();

	public static void staticThrowsException() throws Exception {
		throw new Exception();
	}

	public static void staticThrowsIOException() throws IOException {
		throw new IOException();
	}

	public void throwsNothing() {
	}

	public void throwsException() throws Exception {
		throw new Exception();
	}

	public void throwsIOException() throws IOException {
		throw new IOException();
	}

	public void throwsTwoExceptions() throws IOException, ClassNotFoundException {
		throw new IOException();
	}

	private void dontTriggerEmptyExceptionHandler() {
	}

	// should fail -- catches E, but E not thrown
	public void testFail() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail2() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			throw new IOException();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail3() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			IOException e = new IOException();
			throw e;
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail4() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			throwsIOException();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- catches E, but E not thrown
	public void testFail5() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			staticThrowsIOException();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should fail -- try catch could throw either of two exceptions, catches E,
	// but E not thrown
	public void testFail6() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			throwsTwoExceptions();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown
	public void testPass() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			throw new Exception();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown indirectly
	public void testPass2() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			throw anException;
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown by method
	public void testPass3() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			throwsException();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but E thrown by static method
	public void testPass4() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			staticThrowsException();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}

	// should pass -- catches E, but RuntimeException caught first
	public void testPass5() {
		try {
			for (int i = 0; i < 1000; i++)
				for (int j = i; j < 1000; j++)
					throwsNothing();
			throwsIOException();
		} catch (RuntimeException e) {
			dontTriggerEmptyExceptionHandler();
		} catch (Exception e) {
			dontTriggerEmptyExceptionHandler();
		}
	}
}
