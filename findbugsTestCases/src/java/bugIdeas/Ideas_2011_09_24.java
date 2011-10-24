package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;


public class Ideas_2011_09_24 {
    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PK {}


@Documented
@TypeQualifier(applicableTo = CharSequence.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface SlashedClassName {

    When when() default When.ALWAYS;
}

@Documented
@SlashedClassName(when = When.NEVER)
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface DottedClassName {
}
    
    public boolean badCheck(@SlashedClassName String slashedClassName, @DottedClassName String dottedClassName) {
        return slashedClassName.equals(dottedClassName);
    }
    
    public boolean badCheck2(@SlashedClassName String slashedClassName, @DottedClassName String dottedClassName) {
        return slashedClassName == dottedClassName;
    }
    
    public boolean badCheck(@PK int x) {
        return x == 5;

    }
    public boolean badCheck2(@PK int x) {
        return x > 5;
    }

}
