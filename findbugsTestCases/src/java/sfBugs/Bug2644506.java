package sfBugs;

public class Bug2644506 {

    static boolean same(String a, String b) {
        if (a == null ^ b == null) return false;
        if (a == null && b == null) return true;
		return a.equals(b);
        }
}
