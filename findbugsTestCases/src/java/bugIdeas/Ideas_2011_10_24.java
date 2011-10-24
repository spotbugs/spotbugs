package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_10_24 {
    
    @Documented
    @TypeQualifier()
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PK {}
    
    
    @NoWarning("TQ")
    @PK int asPK(int v) {
        return v;
    }
    @ExpectWarning("TQ")
    @PK int asPK(boolean b, int v) {
        if (b)
            System.out.println("true");
        return v;
    }
    @NoWarning("TQ")
    boolean isFive(@PK int v) {
        return v == 5;
    }
    @ExpectWarning("TQ")
    boolean isFive(@PK int v, int x) {
        return v == x;
    }

   
}
