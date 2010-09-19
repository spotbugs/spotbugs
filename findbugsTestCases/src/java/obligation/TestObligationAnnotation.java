package obligation;

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;

@CleanupObligation
public class TestObligationAnnotation {

    public static @CreatesObligation
    TestObligationAnnotation make() {
        return new TestObligationAnnotation();
    }

    void process() throws IOException {

    }

    public @DischargesObligation
    void done() {

    }

    public static void test1() throws IOException {
        TestObligationAnnotation o = make();
        o.process();
    }

    public static void test2() throws IOException {
        TestObligationAnnotation o = make();
        o.process();
        o.done();
    }

    public static void test3() {
        TestObligationAnnotation o = make();
        try {
            o.process();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            o.done();

        }
    }

}
