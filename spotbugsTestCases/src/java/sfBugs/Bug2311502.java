package sfBugs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.CheckForNull;
import com.github.spotbugs.jsr305.annotation.Nonnull;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug2311502 {

    @Documented
    @Nonnull
    @TypeQualifierDefault(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ReturnValuesAreNonnullByDefault {
    }

    /**
     * Should flag code as unsafe.
     */
    static public class NonNullFalseNegative {

        @CheckForNull
        private Object junkField;

        public void setJunk(Object junk) {
            this.junkField = junk;
        }

        public final class BadInnerClass {
            @ExpectWarning("NP")
            public void badMethod() {
                System.out.println(junkField.hashCode()); // should be caught as
                                                          // a bug
            }
        }

    }

    static public @ReturnValuesAreNonnullByDefault
    class NPNonNullReturnViolationBug {

        @CheckForNull
        private Object junkField;

        public void setJunk(Object junk) {
            this.junkField = junk;
        }

        public final class InnerClass {
            /**
             * Prints out {@link NPNonNullReturnViolationBug#junkField}, if it's
             * currently not <code>null</code>.
             */
            @ExpectWarning("IMA_INEFFICIENT_MEMBER_ACCESS")
            public void printJunk() {
                Object temp = junkField;
                if (temp != null) { // should be perfectly safe
                    System.out.println(temp.hashCode());
                }
            }
        }

    }

}
