package infiniteLoop;

import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class InfiniteLoop {

    int x;

    @ExpectWarning("UrF")
    int y;

    @ExpectWarning("IL")
    void report() {
        report();
    }

    @NoWarning("IL")
    void report2(Object a, Object b) {
        if (a.equals(b)) // we miss this one because we assume equals can do
                         // a store
            report2(a, b);
    }

    @ExpectWarning("IL")
    static void report3(InfiniteLoop obj) {
        InfiniteLoop.report3(obj);
    }

    @NoWarning("IL")
    void doNotReport(Object a, Object b) {
        if (a.equals(b)) {
            doNotReport(b, a);
        }
    }

    @NoWarning("IL")
    void doNotReport2(Object a, Object b) {
        if (x == 0) {
            x = 1;
            // A field has been checked and modified
            doNotReport2(a, b);
        }
    }

    @NoWarning("IL")
    void doNotReport3(Object a, Object b) {
        if (opaque()) {
            // Assume method invocation reads and writes all fields
            doNotReport3(a, b);
        }
    }

    void report4(Object a, Object b) {
        if (x == 0) {
            y = 1;
            // no field has been both read and written!
            report4(a, b);
        }
    }

    @ExpectWarning("IL")
    void report5(List<Object> list) {
        list.add(list);
    }

    protected abstract boolean opaque();
}
