package sfBugs;

public class Bug3107916 {

    public static void main(String[] args) {
        String s1 = null;
        String s2 = "";
        doCompare(s1, s2);
    }

    public static int doCompare(String s1, String s2) {
        int result;
        if (s1 == null && s2 == null) {
            result = 0;
        } else if (s1 == null && s2 != null) {
            result = -1;
        } else if (s1 != null && s2 == null) {
            result = 1;
        } else {
            result = s1.compareTo(s2); // <- this strings is considered to be a
                                       // possible null pointer dereference of
                                       // s1.
        }
        return result;
    }


    public static int doCompare2(String s1, String s2) {
        int result;
        if (s1 == null && s2 == null) {
            result = 0;
        } else if (s1 == null) {
            assert s2 != null;
            result = -1;
        } else if (s2 == null) {
            assert s1 != null;
            result = 1;
        } else {
            result = s1.compareTo(s2); // <- this strings is considered to be a
                                       // possible null pointer dereference of
                                       // s1.
        }
        return result;
    }
}
