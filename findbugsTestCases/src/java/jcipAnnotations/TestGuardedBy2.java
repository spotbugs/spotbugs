package jcipAnnotations;

import javax.annotation.concurrent.GuardedBy;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class TestGuardedBy2 {

    @GuardedBy("this")
    int x;

    int y;

    int z;

    public void setX(int v) {
        x = v;
    }

    public void setY(int v) {
        y = v;
    }

    public synchronized void setZ(int v) {
        z = v;
    }

    @ExpectWarning("IS_FIELD_NOT_GUARDED")
    public int getXY() {
        return x + y;
    }

    public synchronized int getYZ() {
        return y + z;
    }
}
