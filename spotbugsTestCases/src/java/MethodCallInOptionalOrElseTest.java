import java.util.Optional;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class MethodCallInOptionalOrElseTest {

    @ExpectWarning("MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE")
    public String doNotCallMethodsOnOptionalOrElse(String arg) {
        return Optional.ofNullable(arg)
                .orElse(defaultValue()); // Error: consider using Optional.orElseGet here
    }

    @ExpectWarning("MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE")
    public long doNotCallMethodsOnOptionalOrElseWithBoxing() {
        return getOptional()
                .map(Function.identity())
                .orElse(System.currentTimeMillis() + 1); // Error: consider using Optional.orElseGet here
    }

    private Optional<Long> getOptional() {
        return Optional.of(1L);
    }

    public String compliantLambda(String arg) {
        return Optional.ofNullable(arg)
                .orElseGet(() -> defaultValue()); // OK
    }

    public String compliantMethodRef(String arg) {
        return Optional.ofNullable(arg)
                .orElseGet(MethodCallInOptionalOrElseTest::defaultValue); // OK
    }

    public String compliantConstant(String arg) {
        return Optional.ofNullable(arg)
                .orElse("SomeConstantValue"); // OK, no method calls
    }

    public String compliantNullValue(String arg) {
        return Optional.ofNullable(arg)
                .orElse(null); // OK, no method calls
    }

    public String compliantConstantConcatenation(String arg) {
        return Optional.ofNullable(arg)
                .orElse("Some" + "Constant" + "Value"); // OK, concatenated by the compiler
    }

    public long compliantBoxing(Long arg) {
        return Optional.ofNullable(arg)
                .orElse(Long.valueOf(42L)); // OK, boxing is allowed
    }

    private static String defaultValue() {
        return "??";
    }

}
