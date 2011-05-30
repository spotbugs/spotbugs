package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3307637 {
    
    @NoWarning("IL_INFINITE_LOOP")
    public static void fp1() {
        for (float x = 1.0f; x < 1000.0f; x++);
    }
    
    @NoWarning("IL_INFINITE_LOOP")
    public static void fp2() {
        for (float x = 1.0f; x < 1000.0f; x+= 1.0f);
    }

}
