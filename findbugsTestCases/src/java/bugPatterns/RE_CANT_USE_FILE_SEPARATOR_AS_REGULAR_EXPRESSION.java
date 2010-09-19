package bugPatterns;

import java.io.File;
import java.util.regex.Pattern;

public class RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION {

    void bug1(String any1, String any2) {
        any1.replaceAll(File.separator, any2);
    }

    void bug2(String any1, String any2) {
        any1.replaceFirst(File.separator, any2);
    }

    void bug(String any1) {
        any1.split(File.separator);
    }

    void bug2(String any1) {
        any1.matches(File.separator);
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

    void bug4() {
        Pattern.compile(File.separator, Pattern.DOTALL);
    }

    void notBug2() {
        Pattern.compile(File.separator, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
    }

    void notBug3() {
        Pattern.compile(File.separator, Pattern.LITERAL | Pattern.DOTALL);
    }
}
