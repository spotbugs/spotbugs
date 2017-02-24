package sfBugsNew;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;

public class Bug1181 {

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static class ClassA {

        @Nullable
        public String methodThatMightReturnNull() {
            if (Boolean.getBoolean("test")) {
                return null;
            } else {
                return "test";
            }
        }

        @Nullable
        public String methodThatAlwaysReturnsNull() {
            return null;
        }

    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static class ClassA2 {

        @CheckForNull
        public String methodThatMightReturnNull() {
            if (Boolean.getBoolean("test")) {
                return null;
            } else {
                return "test";
            }
        }

        @CheckForNull
        public String methodThatAlwaysReturnsNull() {
            return null;
        }

    }
    
    @NoWarning("NP")
    public void test() {
        ClassA classA = new ClassA();
        String nullable = classA.methodThatMightReturnNull();

        // would expect findbugs warning about possible null dereferencing here
        boolean empty = nullable.isEmpty();
        System.out.println(empty);

        String nullable2 = classA.methodThatAlwaysReturnsNull();


        // would expect findbugs warning about possible null dereferencing here
        empty = nullable2.isEmpty();
        System.out.println(empty);

        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(classA.methodThatMightReturnNull());
        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(nullable);

        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(classA.methodThatAlwaysReturnsNull());
        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(nullable2);

    }

    @ExpectWarning(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", num=4)
    public void test2() {
        ClassA2 classA2 = new ClassA2();
        String maybeNull = classA2.methodThatMightReturnNull();


        // would expect findbugs warning about possible null dereferencing here
        boolean empty = maybeNull.isEmpty();
        System.out.println(empty);

        String nullForSure = classA2.methodThatAlwaysReturnsNull();

        // would expect findbugs warning about possible null dereferencing here
        empty = nullForSure.isEmpty();
        System.out.println(empty);

        // No warning, since method argument is flagged as @Nullable
        methodThatTakesAStringArgumentButNotNull(classA2.methodThatMightReturnNull());

        // Warning expected
        methodThatTakesAStringArgumentButNotNull2(classA2.methodThatMightReturnNull());


     // No warning, since method argument is flagged as @Nullable
        methodThatTakesAStringArgumentButNotNull(classA2.methodThatAlwaysReturnsNull());

        // Warning expected
        methodThatTakesAStringArgumentButNotNull2(classA2.methodThatAlwaysReturnsNull());


    }
    
    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void methodThatTakesAStringArgumentButNotNull(@Nullable String string) {
        String lowerCase = string.trim();
        System.out.println(lowerCase);
    }
    
    public void methodThatTakesAStringArgumentButNotNull2(String string) {
        String lowerCase = string.trim();
        System.out.println(lowerCase);
    }

}
