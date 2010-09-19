package npe;

public class IdentifyCorrectBranchPlease {

    int f(boolean b) {
        Object x = null;
        if (b) {
            x = new Object();
        } else {
            b = true;
        }
        return x.hashCode();
    }

    int f(boolean b1, boolean b2) {
        Object x = null;
        if (b1) {
            x = new Object();
        } else {
            b1 = true;
        }
        if (b2)
            System.out.println("b2 is true");
        return x.hashCode();
    }

}
