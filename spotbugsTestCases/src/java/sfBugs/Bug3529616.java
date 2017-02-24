package sfBugs;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3529616 {
    @NoWarning(value = "SE_BAD_FIELD_INNER_CLASS", confidence=Confidence.MEDIUM)
    private List<String> myList = new ArrayList<String>() {
        {
        add("text1");
        add("text2");
        }
        };
}
