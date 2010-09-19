package sfBugs;

/**
 * Test for bug 1890955 at
 * 
 * https://sourceforge.net/tracker/index.php?func=detail&aid=1890955&group_id=
 * 96405&atid=614693
 * 
 * If the line "System.out.println("hi bugs!");" is commented out, then NPE in
 * OpcodeStack disappears.
 * 
 * @author Andrei
 * 
 */
public class Bug1890955 {

    public void simpleTestCase() {
        Object[] elements = new Object[10];
        for (int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements.length; j++) {
                try {
                    System.out.println("hi bugs!");
                } catch (Throwable e) {
                    continue;
                }
            }
        }
    }

}
