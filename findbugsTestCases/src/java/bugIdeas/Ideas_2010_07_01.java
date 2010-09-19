package bugIdeas;

import java.util.regex.Pattern;

public class Ideas_2010_07_01 {

    public static void main(String args[]) {
        Pattern.compile("+");
        Pattern.compile("+", Pattern.CANON_EQ);
        Pattern.compile("+", Pattern.CASE_INSENSITIVE);
        Pattern.compile("+", Pattern.COMMENTS);
        Pattern.compile("+", Pattern.DOTALL);
        Pattern.compile("+", Pattern.LITERAL); // Not an error
        Pattern.compile("+", Pattern.MULTILINE);
        Pattern.compile("+", Pattern.UNICODE_CASE);
        Pattern.compile("+", Pattern.UNIX_LINES);
        Pattern.compile("+", Pattern.UNICODE_CASE | Pattern.UNIX_LINES);

    }

}
