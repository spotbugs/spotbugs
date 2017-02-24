package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2012_09_27 {

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @TypeQualifier
    public @interface PK {
        
    }

    @PK static int asPK(int x) { return x; };
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @TypeQualifier
    public @interface PK2 {
    }
    
    
    static {
        System.out.println("Initializing class");
        
    }
    @PK static final  int z = asPK(42)+1;
    long x;

    
    @PK int getZ() {
        return z;
    }
  @PK
  long getX() {
      return x;
  }
    @PK
    long getLongKey() {
        return 42;
    }
    
    @PK Object getO() {
        return "42";
    }
    
    @PK
    int getLongKey(int x) {
        return 42+x;
    }

    @ExpectWarning("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED")
    @PK
    long getLongKey3(@PK2 long x) {
        return x;
    }
    
    @ExpectWarning("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED")
    @PK
    long getLongKey(String s) {
        return s.length();
    }

    @NoWarning("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED")
    @PK
    long getLongKey2() {
        return 42L;
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
    @NoWarning("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED")
    @PK
    double getDoubleKey2() {
        return 42.0;
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
