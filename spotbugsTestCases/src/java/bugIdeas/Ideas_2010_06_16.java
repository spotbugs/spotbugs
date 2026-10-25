package bugIdeas;

import edu.umd.cs.findbugs.annotations.NoWarning;

import org.jspecify.annotations.Nullable;

public class Ideas_2010_06_16 {

    @NoWarning("NP")
    static void test2(@Nullable Object... t) {
        for (Object x : t) {
            System.err.println(x);
        }
    }

    public static void main(String args[]) {
        test2("a", "b");
        main2();
    }

    public static void main2() {
        test2((Object[]) null);

    }

}
