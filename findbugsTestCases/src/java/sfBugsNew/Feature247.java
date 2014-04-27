package sfBugsNew;

import com.google.common.base.Optional;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Feature247 {

    @ExpectWarning("NP_OPTIONAL_RETURN_NULL")
    public static <T> Optional<T> returnOptional() {
        // TODO we should compile FB with 1.8
        return null;
    }

    @ExpectWarning("NP_BOOLEAN_RETURN_NULL")
    public static Boolean returnBoolean() {
        return null;
    }
}
