package bugIdeas;

import com.github.spotbugs.jsr305.annotation.Nullable;
import com.github.spotbugs.jsr305.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Ideas_2010_08_06 {

    public static void f(Object x) {

    }

    private static void g(Object x) {

    }

    public static void main(String args[]) {
        f(null);
        g(null);
    }

    public static void test(@Nullable Object x, @Nullable Object y) {
        f(x);
        g(y);
    }

}
