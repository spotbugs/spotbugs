package bugIdeas;

import java.util.Date;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2008_11_21 {

    void whatDoIDo(String reason) {
        throw new RuntimeException(reason);
    }

    static final int FOO = 17;

    int flags;

    boolean isFoo() {
        return (flags & FOO) != 0;
    }

    @ExpectWarning("BSHIFT")
    boolean testShift(int x) {
        return x == x >>> 32;
    }

    String testDeadStorePastUnconditionalThrower() {
        String foo = new Date().toString();
        whatDoIDo("huh");
        return foo;
    }

}
