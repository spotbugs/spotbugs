package sfBugs;

public class Bug2897424 {

    private static final int posCtlBioInd = 56;

    public int notBug(String s) {
        if (s == null)
            return 0;
        return s.hashCode();
    }

    public static boolean notBug(int index) {
        if (index == posCtlBioInd)
            return true;
        return false;
    }

}
