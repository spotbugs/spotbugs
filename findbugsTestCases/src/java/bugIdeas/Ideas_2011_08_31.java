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
    public @interface PK {
    }

    @NoWarning("TQ")
    static @PK
    Integer asPK(Integer i) {
        return i;
    }

    @PK
    int key;

    int foo;

    @ExpectWarning("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED")
    @PK
    int getKey() {
        return foo;
    }

    @ExpectWarning("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED")
    @PK
    int getKey2() {
        return "x".hashCode();
    }

    @ExpectWarning("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED")
    void setKey(int k) {
        key = k;
    }

    @NoWarning("TQ")
    void setKey2(int k) {
        key = asPK(k);
    }

}
