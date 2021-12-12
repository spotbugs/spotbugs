import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class MethodCallInOptionalOrElseTest {

    public static final Set<String> EMPTY_STRING_SET = Collections.emptySet();

    private static Optional<String> STATIC_FIELD;

    private Optional<String> field;

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

    @ExpectWarning("MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE")
    public String variableAssign(String arg) {
        final String variable;
        return Optional.ofNullable(arg)
                .orElse(variable = defaultValue()) + variable; // Error: consider using Optional.orElseGet here
    }

    @ExpectWarning("MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE")
    public String localVariableLoad(String arg) {
        Optional<String> variable = Optional.ofNullable(arg);
        return variable.orElse(defaultValue()); // Error: consider using Optional.orElseGet here
    }

    @ExpectWarning("MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE")
    public String fieldLoad(String arg) {
        field = Optional.ofNullable(arg);
        return field.orElse(defaultValue()); // Error: consider using Optional.orElseGet here
    }

    @ExpectWarning("MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE")
    public String staticFieldLoad(String arg) {
        STATIC_FIELD = Optional.ofNullable(arg);
        return STATIC_FIELD.orElse(defaultValue()); // Error: consider using Optional.orElseGet here
    }

    @ExpectWarning("MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE")
    public String parameterLoad(Optional<String> arg) {
        return arg
                .map(Function.identity())
                .orElse(defaultValue()); // Error: consider using Optional.orElseGet here
    }

    private Optional<Long> getOptional() {
        return Optional.of(1L);
    }

    public String compliantLambda(String arg) {
        return Optional.ofNullable(arg)
                .orElseGet(() -> defaultValue()); // OK, Optional.orElseGet used
    }

    public String compliantMethodRef(String arg) {
        return Optional.ofNullable(arg)
                .orElseGet(MethodCallInOptionalOrElseTest::defaultValue); // OK, Optional.orElseGet used
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

    public String localVariableVoidMethod(String arg) {
        Optional<String> variable = Optional.ofNullable(arg);
        voidMethod(); // OK, regular method call
        return variable.orElse(null);
    }

    public String localVariableStringMethod(String arg) {
        Optional<String> variable = Optional.ofNullable(arg);
        String str = defaultValue(); // OK, extracted to a variable
        return variable.orElse(str);
    }

    public static Set<String> nonNullStringSet(Set<String> set) {
        return Optional.ofNullable(set)
                .orElse(EMPTY_STRING_SET); // OK, constant field
    }

    public static void hiddenVariable(URL url) {
        try (InputStream in = url.openStream()) {
            final Properties props = new Properties();
            props.load(in);
        } catch (IOException ignored) {
        }
    }

    private static String defaultValue() {
        return "??";
    }

    private static void voidMethod() {
    }

}
