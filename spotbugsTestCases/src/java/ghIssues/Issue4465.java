
package ghIssues;

import java.math.BigDecimal;
import java.util.Collections;

public class Issue4465 {

    public static BigDecimal direct() {
        return new BigDecimal(0.1);
    }

    public static BigDecimal throughSingletonList() {
        return new BigDecimal(Collections.singletonList(0.1).get(0));
    }
}
