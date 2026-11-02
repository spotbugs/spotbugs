package ghIssues;

import java.util.Collection;
import java.util.stream.Stream;

public class Issue3752 {
    public static String[] streamBadCast(Stream<String> stream) {
        return (String[]) stream.toArray(); // should report a warning
    }
    
    public static String[] collectionBadCast(Collection<String> collection) {
        return (String[]) collection.toArray(); // should report a warning
    }
}
