import java.io.*;

class IgnoredException {

	void foo(OutputStream o) {
		try {
			System.out.println("Foo");
			o.close();
		} catch (IOException e) {
		}

		try {
			System.out.println("Foo");
			o.close();
		} catch (IOException e) {
			// Just ignore it
		}
		try {
			System.out.println("Foo");
			o.close();
		} catch (Throwable e) {
		}
	}
}
