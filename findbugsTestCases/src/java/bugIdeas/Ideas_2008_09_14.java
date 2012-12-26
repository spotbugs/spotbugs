package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2008_09_14 {

    @ExpectWarning(value="BC_IMPOSSIBLE_CAST", num = 1)
    public String foo(Object o) {
        if (Integer.class.isInstance(o))
            return (String) o;
        return "";
    }

}
