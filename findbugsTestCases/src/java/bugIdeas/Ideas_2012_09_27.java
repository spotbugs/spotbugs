package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2012_09_27 {

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @TypeQualifier
    public @interface PK {
    }

    @PK
    long getLongKey() {
        return 42;
    }

    void setLongKey(@PK long key) {

    }

    @PK
    Long getBoxedLongKey() {
        return 42L;
    }

    void setBoxedLongKey(@PK Long key) {

    }

    @PK
    double getDoubleKey() {
        return 42;
    }

    void setDoubleKey(@PK double key) {

    }

    @PK
    Double getBoxedDoubleKey() {
        return 42.0;
    }

    void setBoxedDoubleKey(@PK Double key) {

    }
    
    @NoWarning("TQ")
    void boxLong() {
        setBoxedLongKey(getLongKey());
    }

    @NoWarning("TQ")
    void unboxLong() {
        setLongKey(getBoxedLongKey());
    }

    @NoWarning("TQ")
    void boxDouble() {
        setBoxedDoubleKey(getDoubleKey());
    }

    @NoWarning("TQ")
    void unboxDouble() {
        setDoubleKey(getBoxedDoubleKey());
    }

}
