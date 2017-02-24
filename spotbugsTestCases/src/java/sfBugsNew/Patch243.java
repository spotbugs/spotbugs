package sfBugsNew;

import java.util.Collection;

public class Patch243 {
    private long fpCountChars(Collection<String> c) {
        long totLength = 0;
        for (String s : c) {
            totLength += s.length();
        }
        return totLength;
    }
}
