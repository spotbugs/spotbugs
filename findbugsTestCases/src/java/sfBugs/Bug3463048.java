package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3463048 {
    private int onlyAssignedInConstructor;
    public Bug3463048() {
        onlyAssignedInConstructor = 10;
    }
    public void something() {
        System.out.println(onlyAssignedInConstructor);
    }
}
