package sfBugsNew;

import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1262 {
    
    @NoWarning("RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION")
    public Pattern falsePositive(String s) {
        Pattern somePattern = Pattern.compile("(?<someGroup>\\w*)");
        return somePattern;
    }

}
