package npe;

public class IgnoreBranchesWhereValueIsKnownNonNull {
    int f(Object x, boolean b) {
        if (x == null)
            System.out.println("x is null");
        if (b)
            System.out.println("b is true");
        // at this point, X is NCP
        if (x != null && b)
            return 0;
        return x.hashCode();
    }

}
