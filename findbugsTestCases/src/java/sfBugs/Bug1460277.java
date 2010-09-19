package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1460277 {
    static Object DOMAINCFG = new Object();

    @NoWarning("MWN")
    void test() throws InterruptedException {
        synchronized (DOMAINCFG) {
            DOMAINCFG.wait();
        }
    }

}
