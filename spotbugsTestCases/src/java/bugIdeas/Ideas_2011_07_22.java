package bugIdeas;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_07_22 {
    
    @DesireNoWarning("NP_NULL_ON_SOME_PATH")
    public int getHashCode(Object x, Object y) {
        Preconditions.checkArgument(x != null && y != null, "arguments must be nonnull");
        return x.hashCode() + y.hashCode();
    }
    
    @DesireWarning("NP_NULL_ON_SOME_PATH")
    public int getHashCode0(Object x) {
        boolean b = x != null;
        if (b)
            System.out.println("Good");
        return x.hashCode();
    }
    @DesireNoWarning("NP_NULL_ON_SOME_PATH")
    public int getHashCode(Object x) {
        Preconditions.checkArgument(x != null, "x is null");
        return x.hashCode();
    }
    
    @NoWarning("NP_NULL_ON_SOME_PATH")
    public int getHashCode2(Object x) {
        Preconditions.checkNotNull(x, "x is null");
        return x.hashCode();
    }

    @NoWarning("NP_NULL_ON_SOME_PATH")
    @ExpectWarning("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public int getHashCode3(Object x) {
        Preconditions.checkNotNull(x, "x is null");
        if (x == null)
            System.out.println("huh?");
        return x.hashCode();
    }
    @NoWarning("NP_NULL_ON_SOME_PATH")
    @ExpectWarning("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public int getHashCode4(Object x) {
        Preconditions.checkNotNull(x);
        if (x == null)
            System.out.println("huh?");
        return x.hashCode();
    }
    @NoWarning("NP_NULL_ON_SOME_PATH")
    @ExpectWarning("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public int getHashCode5(Object x) {
        Preconditions.checkNotNull(x, "x is null %d", 42);
        if (x == null)
            System.out.println("huh?");
        return x.hashCode();
    }

    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE,RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public int getHashCode6(@Nullable Object x) {
        Preconditions.checkNotNull(x, "x is null %d", 42);
        if (x == null)
            System.out.println("huh?");
        return x.hashCode();
    }
    
    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public int getHashCode7(@Nullable Object x) {
        Preconditions.checkNotNull(x, "x is null %d", 42);
       return 42;
    }
    
}
