/* generates

 TestingGround: [0002]  if_icmpge
 TestingGround: [0005]  iconst_1
 TestingGround: [0006]  ireturn
 TestingGround: [0007]  iconst_0
 TestingGround: [0008]  ireturn

 */

public class TestBooleanReturn {
    public boolean test1(int a, int b) {
        if (a < b)
            return true;
        else
            return false;
    }

    public boolean test2(int a, int b) {
        // this should not be reported
        while (a < b) {
            a++;
            if (a == 0)
                return true;
        }
        return false;
    }

    public boolean test3(int a, int b) {
        // this should not be reported
        switch (a) {
        case 0:
            if (b == 0)
                return true;
        case 1:
            return false;

        case 2:
            switch (b) {
            case 0:
                if (a == b)
                    return false;

            case 1:
                return true;
            }
            break;

        case 3:
            if (b == 1)
                return true;

        default:
            return false;
        }

        return false;
    }
}
