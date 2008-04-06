package sfBugs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Bug1936055 {
	public static void main(String args[]) {
		try {
			String id = (new BufferedReader(new FileReader("tmp"))).readLine();
			System.out.println("CourseMembership " + id + " not found (ignored)");
		} catch (IOException e) {
			// comment (no warning reported here *unless* -Dfindbugs.de.comment=true)
		}
	}
}
