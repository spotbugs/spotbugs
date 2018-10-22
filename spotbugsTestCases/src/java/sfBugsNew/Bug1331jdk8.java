package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1331jdk8 {

    // @NoWarning("VA_FORMAT_STRING_BAD_CONVERSION")
    public static void main(String[] args) {
        System.out.println(String.format("Today is %1$tm/%1$td/%1$tY",
                java.time.LocalDate.now()));
    }
}
