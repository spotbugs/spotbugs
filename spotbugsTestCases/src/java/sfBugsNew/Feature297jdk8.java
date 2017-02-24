package sfBugsNew;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Feature297jdk8 {

    @ExpectWarning("NP_OPTIONAL_RETURN_NULL")
    public static <T> Optional<T> returnOptional() {
        return null;
    }

    @ExpectWarning("NP_OPTIONAL_RETURN_NULL")
    public static <T> com.google.common.base.Optional<T> returnGoogleOptional() {
        return null;
    }

    @ExpectWarning("NP_BOOLEAN_RETURN_NULL")
    public static Boolean returnBoolean() {
        return null;
    }
}
