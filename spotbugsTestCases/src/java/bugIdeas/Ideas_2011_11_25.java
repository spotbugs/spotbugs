package bugIdeas;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

import org.junit.jupiter.api.Assertions;

public class Ideas_2011_11_25 {

    @NoWarning("DMI_ARGUMENTS_WRONG_ORDER")
    public void foo(String x) {
        Assertions.assertNotNull("x must be nonnull", x);
    }

    @ExpectWarning("DMI_ARGUMENTS_WRONG_ORDER")
    public void bar(String x) {
        Preconditions.checkNotNull("x must be nonnull", x);
    }

    @ExpectWarning("DMI_DOH")
    public void bar2(String x) {
        Preconditions.checkNotNull("x must be nonnull");
    }

    @NoWarning("DMI_ARGUMENTS_WRONG_ORDER")
    public void bar3(String x) {
        Preconditions.checkNotNull(x, "x must be nonnull");
    }

}
