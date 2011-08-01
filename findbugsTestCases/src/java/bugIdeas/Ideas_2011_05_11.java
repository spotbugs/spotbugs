package bugIdeas;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_05_11 {
    
    @NoWarning("RE")
    static String getAsSpaces(String txt) {
        return txt.replaceAll(".", " ");
    }

}
