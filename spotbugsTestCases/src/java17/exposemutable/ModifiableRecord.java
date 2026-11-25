package exposemutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Counterpart of {@link UnmodifiableRecord}: one of the values of each component is modifiable, so each accessor exposes it.
 */
public record ModifiableRecord(List<String> list, Set<String> set, List<String> ifList, Map<String, String> ifMap) {

    public ModifiableRecord {
        list = list != null ? List.copyOf(list) : new ArrayList<>();
        set = set != null ? new HashSet<>(set) : Collections.emptySet();

        if (ifList != null) {
            ifList = List.copyOf(ifList);
        } else {
            ifList = new ArrayList<>();
        }

        if (ifMap != null) {
            ifMap = new HashMap<>(ifMap);
        } else {
            ifMap = Collections.emptyMap();
        }
    }
}
