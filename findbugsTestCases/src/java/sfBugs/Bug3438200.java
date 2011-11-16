package sfBugs;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Bug3438200 {

    interface UsageDataEventWrapper {
    }

    Map<UsageDataEventWrapper, String> usageDataColumnProvider = new HashMap<UsageDataEventWrapper, String>();

    Comparator<UsageDataEventWrapper> comparator = new Comparator<UsageDataEventWrapper>() {
        public int compare(UsageDataEventWrapper event1, UsageDataEventWrapper event2) {
            if (usageDataColumnProvider == null)
                return 0;
            String text1 = usageDataColumnProvider.get(event1);
            String text2 = usageDataColumnProvider.get(event2);

            if (text1 == null && text2 == null)
                return 0;
            if (text1 == null)
                return -1;
            if (text2 == null)
                return 1;

            return text1.compareTo(text2);
        }
    };

}
