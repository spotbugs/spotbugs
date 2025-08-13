package ghIssues;

public class Issue3455 {
    public int withDup(boolean b1, String input) {
        Object x = (input == null ? new Object() : null);

        if (input != null && input.length() > 5) { x = null; }
        if (input != null && input.length() > 5) { x = null; } 

        if (b1) return x.hashCode();   
        return -x.hashCode();            
    }
}