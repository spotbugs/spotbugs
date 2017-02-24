package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1298 {

    @DesireNoWarning("SF_SWITCH_NO_DEFAULT")
    public void testFallthrough(int x) {
        switch (x) {
        case 1:
          System.out.println("irrelevant");
          //$FALL-THROUGH$
        default:
          System.out.println("irrelevant");
          break;
        }
      }
    
    @NoWarning("SF_SWITCH_NO_DEFAULT")
    public void testFallthrough2(int x) {
        switch (x) {
        case 0:
            System.out.println("boring");
            break;
        case 1:
          System.out.println("irrelevant");
          //$FALL-THROUGH$
        default:
          System.out.println("irrelevant");
          break;
        }
      }
}
