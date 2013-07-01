package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1187 {

    private boolean condition;

    private final Object object = new Object();

    public void setCondition() {
        synchronized (object) {
            condition = true;
            object.notifyAll();
        }
    }

    @ExpectWarning("UW_UNCOND_WAIT")
    public void noLoopOrTest() throws Exception {

        synchronized (object) {
            object.wait();
        }
    }

    @ExpectWarning("WA_NOT_IN_LOOP")
    public void noLoop() throws Exception {

        synchronized (object) {
            if (!condition)
                object.wait();
        }
    }

    @NoWarning("WA_NOT_IN_LOOP,UW_UNCOND_WAIT")
    public void whileDo() throws Exception {

        synchronized (object) {
            while (!condition) {
                object.wait();
            }
        }
    }

    @ExpectWarning("UW_UNCOND_WAIT")
    public void doWhile() throws Exception {

        synchronized (object) {
            do {
                object.wait();
            } while (!condition);
        }
    }
}
