package tomcat;

import java.util.Collection;
import java.util.List;

public class CollectionUtils {
    public static List doNotReport(Collection collection) {
        if (collection instanceof List) {
            return (List) collection;
        } else
            return null;
    }

}
