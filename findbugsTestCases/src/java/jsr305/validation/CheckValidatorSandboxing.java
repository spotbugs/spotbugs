package jsr305.validation;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.TypeQualifierValidator;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class CheckValidatorSandboxing {

    void needsEven(@Even int x) {
    }

    void needsEven(@Even long x) {
    }

    @NoWarning("TQ")
    void testOK(@Even int x) {
        needsEven(-2);
        needsEven(0);
        needsEven(2);
        needsEven(2L);
        needsEven(x);
    }

    @ExpectWarning(value = "TQ", num=5)
    void testBad(int x) {
        needsEven(-1);
        needsEven(1);
        needsEven(3);
        needsEven(3L);
        needsEven(x);
    }

    @Documented
    @TypeQualifier(applicableTo = Number.class)
    @Retention(RetentionPolicy.RUNTIME)
    static public @interface Even {
        static class Checker implements TypeQualifierValidator<Even> {

            @Override
            public When forConstantValue(Even annotation, Object value) {
                try {
                    String[] rootList = new File("/").list();
                    throw new RuntimeException("Got list of root files " + Arrays.toString(rootList));
                } catch (SecurityException e) {
                    // e.printStackTrace();
                    assert true;
                }
                try {
                    Class c = Class.forName("edu.umd.cs.findbugs.FindBugs2");
                    throw new RuntimeException("Should not have been able to load " + c);
                } catch (ClassNotFoundException e) {
                    assert true;
                } catch (SecurityException e) {
                    assert true;
                }
                if (value instanceof Number) {
                    if (((Number) value).longValue() % 2 == 0)
                        return When.ALWAYS;
                    else
                        return When.NEVER;
                } else
                    return When.UNKNOWN;
            }

        }

    }

}
