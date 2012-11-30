package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_08_31 {
    
    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PK {}
    
    static @PK Integer asPK(Integer i) {
        return i;
    }
    

    @PK int key;
    
    @ExpectWarning("TQ_NEVER_VALUE_USED_WHERE_ALWAYS_REQUIRED")
    void setKey(int k) {
        key =k;
    }

    @NoWarning("TQ_NEVER_VALUE_USED_WHERE_ALWAYS_REQUIRED")
    void setKey2(int k) {
        key = asPK(k);
    }


}
