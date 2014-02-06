package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1249 {

    @ExpectWarning("SF_SWITCH_FALLTHROUGH")
    void foo1(int x) {

        switch (x) {
        case 0:
            System.out.println("0");
            break;
        case 1:
            System.out.println("1");
        case 2:
            System.out.println("2");
            break;
       
        default:
            System.out.println("?");
            break;
        }
    }
    
    @DesireNoWarning("SF_SWITCH_FALLTHROUGH")
    void foo2(int x) {

        switch (x) {
        case 0:
            System.out.println("0");
            break;
        case 1:
            System.out.println("1");
            // fallthrough
        case 2:
            System.out.println("2");
            break;
       
        default:
            System.out.println("?");
            break;
        }
    }
    
    @DesireNoWarning("SF_SWITCH_FALLTHROUGH")
    void foo3(int x) {

        switch (x) {
        case 0:
            System.out.println("0");
            break;
        case 1:
            System.out.println("1");             // fallthrough
        case 2:
            System.out.println("2");
            break;
       
        default:
            System.out.println("?");
            break;
        }
    }
}
