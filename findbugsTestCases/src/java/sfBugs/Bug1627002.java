package sfBugs;

import java.util.Arrays;

public class Bug1627002 {
	public static void main(String args[]) {
		testToStringOnArray(null);
	}
	public static void testToStringOnArray(String [] mimeTypes) {
		// Findbugs 1.1.3 rc1 thinks: DMI_INVOKING_TOSTRING_ON_ARRAY
		System.out.println("mimeTypes=" + mimeTypes == null
		? "null"
		: Arrays.asList(mimeTypes).toString());

		}
}
