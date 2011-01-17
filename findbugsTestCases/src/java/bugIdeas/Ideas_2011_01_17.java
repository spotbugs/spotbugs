package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_01_17 {
    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PK {
    }

    @PK int key = 17;
    @NoWarning("TQ")
    @PK int getKeyAsInt() {
        return key;
    }
    @NoWarning("TQ")
    @PK Integer getKeyAsInteger() {
        return key;
    }
    @NoWarning("TQ")
    void setKeyAsInt(@PK int key) {
        this.key = key;
    }
    @NoWarning("TQ")
    void setKeyAsInteger(@PK Integer key) {
        this.key = key;
    }
    @NoWarning("TQ")
    static void test(Ideas_2011_01_17 x, Ideas_2011_01_17 y) {
        x.setKeyAsInt(y.getKeyAsInt());
        x.setKeyAsInt(y.getKeyAsInteger());
        x.setKeyAsInteger(y.getKeyAsInt());
        x.setKeyAsInteger(y.getKeyAsInteger());
    }


}
