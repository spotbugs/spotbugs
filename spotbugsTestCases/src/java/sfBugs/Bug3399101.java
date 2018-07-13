package sfBugs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.CheckForNull;
import com.github.spotbugs.jsr305.annotation.Nonnull;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierDefault;

import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import sfBugs.Bug3399101.ParametersAreCheckForNullByDefault;

@DefaultAnnotationForParameters(CheckForNull.class)
@ParametersAreCheckForNullByDefault
public class Bug3399101 {


    @Documented
    @CheckForNull
    @TypeQualifierDefault(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public
    @interface ParametersAreCheckForNullByDefault {
    }

        @Nonnull
        public Object field;

        @ExpectWarning("NP")
        public Bug3399101(Object param) {
                field = param;
        }

        @ExpectWarning("NP")
        public void setField(Object param) {
                field = param;
        }

        @ExpectWarning("NP")
        public void setField2(@CheckForNull Object param) {
                field = param;
        }

        @ExpectWarning("NP")
        @Nonnull
        public static Object getParam(Object param) {
                return param;
        }

        @ExpectWarning("NP")
        public int getHash(Object param) {
                return param.hashCode();
        }
}