
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Getter {
    @ExpectWarning("IS2_INCONSISTENT_SYNC")
    int x;

    @ExpectWarning("UG_SYNC_SET_UNSYNC_GET")
    public int getX() {
        return x;
    }

    public synchronized void setX(int x) {
        this.x = x;
    }

    public synchronized int calculate() {
        int t = 0;
        t += x;
        t += x;
        return t;
    }

    @NoWarning("IS2_INCONSISTENT_SYNC")
    @javax.annotation.concurrent.GuardedBy("this")
    int y;

    public synchronized int getY(){
        return y;
    }

    @ExpectWarning("IS_FIELD_NOT_GUARDED")
    public void setY(int y){
        this.y = y;
    }

    @NoWarning("IS2_INCONSISTENT_SYNC")
    @net.jcip.annotations.GuardedBy("this")
    int z;

    public synchronized int getZ(){
        return z;
    }

    @ExpectWarning(value="IS_FIELD_NOT_GUARDED")
    public void setZ(int z){
        this.z = z;
    }
}
