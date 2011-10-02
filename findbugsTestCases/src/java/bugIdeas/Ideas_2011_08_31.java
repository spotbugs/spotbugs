package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

public class Ideas_2011_08_31 {
    
    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PK {}
    
    static @PK Integer asPK(Integer i) {
        return i;
    }
    

    @PK int key;
    
    void setKey(int k) {
        key =k;
    }

    void setKey2(int k) {
        key = asPK(k);
    }


}
