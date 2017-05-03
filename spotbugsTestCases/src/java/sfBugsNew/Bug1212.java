package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1212 {

    @NoWarning("TESTING")
    @ExpectWarning("DM_BOXED_PRIMITIVE_TOSTRING")
    public String doSomething() {
        return Integer.valueOf(0).toString();
        }
}
