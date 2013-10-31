package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1212 {

    @DesireNoWarning("TESTING")
    public String doSomething() {
        return Integer.valueOf(0).toString();
        }
}
