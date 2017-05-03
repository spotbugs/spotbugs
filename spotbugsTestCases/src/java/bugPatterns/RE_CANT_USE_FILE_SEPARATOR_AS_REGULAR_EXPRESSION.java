package bugPatterns;

import java.io.File;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION {

    @ExpectWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void bug1(String any1, String any2) {
        any1.replaceAll(File.separator, any2);
    }

    @ExpectWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void bug2(String any1, String any2) {
        any1.replaceFirst(File.separator, any2);
    }

    @ExpectWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void bug(String any1) {
        any1.split(File.separator);
    }

    @ExpectWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void bug2(String any1) {
        any1.matches(File.separator);
    }

    @ExpectWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void bug2() {
        Pattern.compile(File.separator);
    }

    @ExpectWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void bug3() {
        Pattern.compile(File.separator, Pattern.CASE_INSENSITIVE);
    }

    @NoWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void notBug() {
        Pattern.compile(File.separator, Pattern.LITERAL);
    }

    @ExpectWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void bug4() {
        Pattern.compile(File.separator, Pattern.DOTALL);
    }

    @NoWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void notBug2() {
        Pattern.compile(File.separator, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
    }

    @NoWarning("RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION")
    void notBug3() {
        Pattern.compile(File.separator, Pattern.LITERAL | Pattern.DOTALL);
    }
}
