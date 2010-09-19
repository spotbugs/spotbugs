package obligation;

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;

@CleanupObligation
public class TestStaticObligationAnnotation {

    public static @CreatesObligation
    TestStaticObligationAnnotation make() {
        return new TestStaticObligationAnnotation();
    }

    void process() throws IOException {

    }

    public static @DischargesObligation
    void done() {

    }

    public static void test1() throws IOException {
        TestStaticObligationAnnotation o = make();
        o.process();
    }

    public static void test2() throws IOException {
        TestStaticObligationAnnotation o = make();
        o.process();
        done();
    }

    public static void test3() {
        TestStaticObligationAnnotation o = make();
        try {
            o.process();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            done();

        }
    }

}
