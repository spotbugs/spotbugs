package sfBugs;

public class Bug1865265 {
    public static String foo(char[] buf) {
        StringBuilder sb = new StringBuilder();
        sb.append(buf, 0, buf.length);
        return sb.toString();
    }
}
