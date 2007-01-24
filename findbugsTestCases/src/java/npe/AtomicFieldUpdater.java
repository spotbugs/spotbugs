package npe;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class AtomicFieldUpdater {
    
    volatile Object x;
    volatile int y;
    volatile long z;
    
    AtomicReferenceFieldUpdater<AtomicFieldUpdater  , Object> updaterX 
     = AtomicReferenceFieldUpdater.newUpdater(AtomicFieldUpdater.class, Object.class, "x");
    
    AtomicIntegerFieldUpdater<AtomicFieldUpdater> updaterY
    = AtomicIntegerFieldUpdater.newUpdater(AtomicFieldUpdater.class, "y");
 
    AtomicLongFieldUpdater<AtomicFieldUpdater> updaterZ
    = AtomicLongFieldUpdater.newUpdater(AtomicFieldUpdater.class,  "z");
 
    int f() {
        return x.hashCode();
    }
    
    void setX(Object foo) {
        updaterX.set(this, foo);
    }
    void setY(int foo) {
        updaterY.set(this, foo);
    }
    
    void setZ(long foo) {
        updaterZ.set(this, foo);
    }

}
