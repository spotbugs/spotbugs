import java.io.IOException;

public abstract class FinallyBlockInlining {

	abstract void foo() throws IOException;

	public void bar() {
		Throwable throwable = null;
		try {
            try {
				try {
					foo();
				} catch (NullPointerException e) {
                    throwable = e;
				} catch (RuntimeException e) {
					throwable = e;
				}
            } catch (IOException e) {
				throwable = e;
			}
		} catch (Error e) {
            throwable = e;
		} finally {
			if (throwable != null) {
				if (throwable instanceof NullPointerException) {
                    NullPointerException e = (NullPointerException) throwable;
					throw e;
				} else if (throwable instanceof RuntimeException) {
					RuntimeException e = (RuntimeException) throwable;
                    throw e;
				} else if (throwable instanceof IOException) {
					RuntimeException e = new RuntimeException(throwable);
					throw e;
                } else if (throwable instanceof Error) {
					Error e = (Error) throwable;
					throw e;
				}
            }
		}
	}

}
