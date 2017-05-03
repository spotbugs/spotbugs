package sfBugs;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Bug3463046 {
    @DesireWarning("")
    private static final AtomicIntegerFieldUpdater<Bug3463046> s_atomic1
            = AtomicIntegerFieldUpdater.newUpdater(Bug3463046.class, "noSuchField");
    private static final AtomicIntegerFieldUpdater<Bug3463046> s_atomic2
            = AtomicIntegerFieldUpdater.newUpdater(Bug3463046.class, "incorrectFieldType");
    private static final AtomicIntegerFieldUpdater<Bug3463046> s_atomic3
            = AtomicIntegerFieldUpdater.newUpdater(Bug3463046.class, "fieldNotVolatile");

    private volatile long incorrectFieldType;
    private int fieldNotVolatile;
}
