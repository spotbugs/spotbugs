package bugPatterns;

import java.io.File;
import java.util.regex.Pattern;

public class RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION {

	void bug1(String any1, String any2) {
		any1.replaceAll(File.separator, any2);
	}
	void bug2() {
		Pattern.compile(File.separator);
	}
	void bug3() {
		Pattern.compile(File.separator, Pattern.CASE_INSENSITIVE);
	}
	void notBug() {
		Pattern.compile(File.separator, Pattern.LITERAL);
	}
	void notBug2 () {
		Pattern.compile(File.separator, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
	}
}
