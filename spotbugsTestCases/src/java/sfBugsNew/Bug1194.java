package sfBugsNew;

import javax.annotation.ParametersAreNonnullByDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import sfBugs.Bug3399101.ParametersAreCheckForNullByDefault;

public class Bug1194 {

    @NoWarning("NP")
    int noNullnessAnnotations(Object x) {
        return x.hashCode();

    }

    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    @ParametersAreCheckForNullByDefault
    int checkForNullByDefault(Object x) {
        return x.hashCode();

    }

    @NoWarning("NP")
    @ParametersAreNonnullByDefault
    int nonnullByDefault(Object x) {
        return 17;
    }

    @ExpectWarning("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public void test1() {
        System.out.println(noNullnessAnnotations(null));
    }

    @NoWarning("NP")
    public void test2() {
        System.out.println(checkForNullByDefault(null));
    }

    @ExpectWarning("NP_NONNULL_PARAM_VIOLATION")
    public void test3() {
        System.out.println(nonnullByDefault(null));
    }

}
