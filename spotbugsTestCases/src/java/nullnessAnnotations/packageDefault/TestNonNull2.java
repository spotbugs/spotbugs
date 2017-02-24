package nullnessAnnotations.packageDefault;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class TestNonNull2 extends TestNonNull1 implements Interface1 {

    @ExpectWarning("NP_NONNULL_PARAM_VIOLATION")
    void report1() {
        f(null); // should get a NonNull warning from TestNonNull1
    }

    @NoWarning("NP")
    void report2() {
        //
        // FindBugs doesn't produce a warning here because the g()
        // method in TestNonNull1 explicitly marks its parameter
        // as @Nullable. So, we shouldn't expect a warning. (?)
        //
        g(null); // should get a NonNull warning from Interface1
    }

    @NoWarning("NP")
    void ok1() {
        h(null); // should be OK
    }

    @ExpectWarning(value="NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", num=1)
    public Object k(@CheckForNull Object o) {
        s = o;
        return o;
    }
}
