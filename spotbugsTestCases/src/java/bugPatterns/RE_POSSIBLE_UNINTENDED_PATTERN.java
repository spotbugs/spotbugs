package bugPatterns;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class RE_POSSIBLE_UNINTENDED_PATTERN {

    @ExpectWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String [] bug1(String any) {
        return any.split(".");
    }

    @ExpectWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String bug2(String any, String any2) {
        return any.replaceAll(".", any2);
    }

    @ExpectWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String bug3(String any, String any2) {
        return any.replaceFirst(".", any2);
    }
    @ExpectWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String [] bug11(String any) {
        return any.split("|");
    }

    @ExpectWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String bug22(String any, String any2) {
        return any.replaceAll("|", any2);
    }

    @ExpectWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String bug33(String any, String any2) {
        return any.replaceFirst("|", any2);
    }

    @ExpectWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String bug4(String any) {
        return any.replaceAll("|", "*");
    }

    @NoWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    String notBug1(String any) {
        return any.replaceAll(".", "*");
    }

    @NoWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    @ExpectWarning("IIO_INEFFICIENT_INDEX_OF")
    int notBug2(String any) {
        return any.indexOf(".");
    }


    @NoWarning("RE_POSSIBLE_UNINTENDED_PATTERN")
    @ExpectWarning("IIO_INEFFICIENT_INDEX_OF")
    int notBug22(String any) {
        return any.indexOf("|");
    }
}
