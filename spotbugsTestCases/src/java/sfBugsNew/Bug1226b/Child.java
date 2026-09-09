package sfBugsNew.Bug1226b;

import edu.umd.cs.findbugs.annotations.NoWarning;
import sfBugsNew.Bug1226a.Super;

public class Child extends Super {
    public String run() {
        new Runnable() {
            @NoWarning("IMA_INEFFICIENT_MEMBER_ACCESS")
            @Override
            public void run() {
                field = field.substring(1);
            }
        }.run();
        return field;
    }
}
