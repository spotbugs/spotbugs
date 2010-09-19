package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3062706 {
    @DesireNoWarning("SBSC_USE_STRINGBUFFER_CONCATENATION")
    void blah() {
        String link = "someValue";
        while (link.equals("someValue")) {
            if (something().equals("foo")) {
                link = link + "extra value";
                break;
            }
        }
        System.out.println(link);
    }

    String something() {
        return null;
    }
}
