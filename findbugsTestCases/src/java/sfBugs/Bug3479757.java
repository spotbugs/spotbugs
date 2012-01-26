package sfBugs;

public class Bug3479757 {
    public interface Frobber {
        void frob(StringBuilder sb);
    }

    public interface Honker {
        void honk(String s);
    }

    public void beep(Frobber frobber, Honker honker) {
        StringBuilder sb = new StringBuilder("honk");
        sb.insert(4, "beep");
        honker.honk(sb.toString());
        frobber.frob(sb);
        honker.honk(sb.toString());
    }
}
