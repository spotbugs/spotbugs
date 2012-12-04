package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2012_12_03 {
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @TypeQualifier
    public @interface PK {
    }
    @Documented
    @TypeQualifier(applicableTo = CharSequence.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SlashedClassName {
        
        public static final String NOT_AVAILABLE = "./.";

        When when() default When.ALWAYS;
    }
    
    @NoWarning("TQ")
    @PK int foo(@PK int x, int y) {
        return x;
    }
    
    @NoWarning("TQ")
    @SlashedClassName String foo() {
        return null;
    }
}
