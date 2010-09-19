package bugPatterns;

public class RE_POSSIBLE_UNINTENDED_PATTERN {

    void bug1(String any) {
        any.split(".");
    }

    void bug2(String any, String any2) {
        any.replaceAll(".", any2);
    }

    void bug3(String any, String any2) {
        any.replaceFirst(".", any2);
    }

    void notBug1(String any) {
        any.replaceAll(".", "*");
    }

    void notBug2(String any) {
        any.indexOf(".");
    }
}
