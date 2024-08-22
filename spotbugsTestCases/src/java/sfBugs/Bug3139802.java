package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3139802 {
    @NoWarning("NP")
    public static void main(String[] args) {
        System.out.println("OK");
    }
}
