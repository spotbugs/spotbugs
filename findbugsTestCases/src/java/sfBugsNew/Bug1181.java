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
        String maybeNull = classA.methodThatMightReturnNull();

        // in case the following is commented in the method local analysis
        // determines that maybeNull might be null below
        // when isEmpty is called

        // if (maybeNull == null) {
        // System.out.print("maybeNull is null");
        // }

        // would expect findbugs warning about possible null dereferencing here
        boolean empty = maybeNull.isEmpty();
        System.out.println(empty);

        String nullForSure = classA.methodThatAlwaysReturnsNull();

        // in case the following is commented in the method local analysis
        // determines that maybeNull might be null below
        // when isEmpty is called

        // if (nullForSure == null) {
        // System.out.print("nullForSure is null");
        // }

        // would expect findbugs warning about possible null dereferencing here
        empty = nullForSure.isEmpty();
        System.out.println(empty);

        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(classA.methodThatMightReturnNull());
        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(maybeNull);

        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(classA.methodThatAlwaysReturnsNull());
        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(nullForSure);

    }

    @ExpectWarning(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", num=4)
    public void test2() {
        ClassA2 classA = new ClassA2();
        String maybeNull = classA.methodThatMightReturnNull();

        // in case the following is commented in the method local analysis
        // determines that maybeNull might be null below
        // when isEmpty is called

        // if (maybeNull == null) {
        // System.out.print("maybeNull is null");
        // }

        // would expect findbugs warning about possible null dereferencing here
        boolean empty = maybeNull.isEmpty();
        System.out.println(empty);

        String nullForSure = classA.methodThatAlwaysReturnsNull();

        // in case the following is commented in the method local analysis
        // determines that maybeNull might be null below
        // when isEmpty is called

        // if (nullForSure == null) {
        // System.out.print("nullForSure is null");
        // }

        // would expect findbugs warning about possible null dereferencing here
        empty = nullForSure.isEmpty();
        System.out.println(empty);

        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(classA.methodThatMightReturnNull());
        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(maybeNull);

        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(classA.methodThatAlwaysReturnsNull());
        // would expect findbugs warning about possibly passing a null value to
        // a method that does not allow null as
        // parameter
        methodThatTakesAStringArgumentButNotNull(nullForSure);

    }
    
    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void methodThatTakesAStringArgumentButNotNull(@Nullable String string) {
        String lowerCase = string.trim();
        System.out.println(lowerCase);
    }

}
