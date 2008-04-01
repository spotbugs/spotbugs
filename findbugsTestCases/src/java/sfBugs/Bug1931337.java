package sfBugs;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class Bug1931337 {

	Set<File> compiling = new HashSet<File>();

	void foo(File file) {
		try {
			System.out.println(file);
		} finally {
			synchronized (compiling) {
				compiling.remove(file);
				compiling.notifyAll();
			}
		}

	}
}
