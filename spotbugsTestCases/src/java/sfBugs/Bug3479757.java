package sfBugs;

public class Bug3479757 {
    public interface Frobber {
        void frob(StringBuilder sb);
    }

    public interface Honker {
        void honk(String s);
    }

    /**
     * FindBugs provides an incorrect static string value on the stack at both
     * calls to honker.honk(). In the second case, it has no information at all
     * about the value of sb.toString() so it shouldn't provide a static value
     * at all. In the first case, ideally it would recognize the value is
     * "honkbeep", but at least not returning any static value would be an
     * improvement over returning "honk".
     */

    public void beep(Frobber frobber, Honker honker) {
        StringBuilder sb = new StringBuilder("honk");
        sb.insert(4, "beep");
        honker.honk(sb.toString());
        frobber.frob(sb);
        honker.honk(sb.toString());
    }
}
