package sfBugs;

public class Bug2817789 {
    public String testStringBuilder(final String what, final int times) {
        String result = "";
        for (int i = 0; i < times; i++)
            result += what;
        return result;
    }
}
