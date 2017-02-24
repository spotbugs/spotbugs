package sfBugs;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class RFE3026970 {

    private String get() {
        return null;
    }

    @DesireWarning("NP")
    public void testDesireWarning() {
        if (get().equals("A"))
            System.out.println("test");
    }

    public void testDesireNoWarning() {
        if ("A".equals(get()))
            System.out.println("test");
    }
}
