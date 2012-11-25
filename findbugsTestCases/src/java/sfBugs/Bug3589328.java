package sfBugs;

import javax.annotation.Nonnull;

import com.google.common.base.Function;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3589328 implements Function{

    @NoWarning("NP")
    @Override
    public Object apply(@Nonnull Object x) {
        if (x.hashCode() == 42)
            return 42;
        return x;
        }

}
 