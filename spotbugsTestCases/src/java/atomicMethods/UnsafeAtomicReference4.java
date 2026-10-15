package atomicMethods;

import java.util.concurrent.atomic.AtomicReference;

public class UnsafeAtomicReference4 {

    private AtomicReference<String> ref = new AtomicReference<>();

    class Inner {

        private AtomicReference<String> innerRef;
        private String somethingElse;

        public Inner(AtomicReference<String> string, String somethingElse) {
            innerRef = string;
            this.somethingElse = somethingElse;
        }
    }

    public void initProcessing() {
        ref = new AtomicReference<>("init");
        Inner inner = new Inner(ref, "somethingElse");
    }

    public synchronized void process() {
        if (ref.get() == null) {
            return;
        }
        ref.set("processing");
    }

    public void finishProcessing() {
        if (ref.get() != null) {
            return;
        }
        System.out.println("finished");
    }
}
