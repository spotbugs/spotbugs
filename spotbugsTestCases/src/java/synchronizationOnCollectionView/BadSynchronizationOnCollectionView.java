package synchronizationOnCollectionView;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressFBWarnings("EI_EXPOSE_REP")
public class BadSynchronizationOnCollectionView {

    private final Map<Integer, String> mapView =
            Collections.synchronizedMap(new HashMap<>());
    private final Set<Integer> setView = mapView.keySet();

    public Map<Integer, String> getMap() {
        return mapView;
    }

    public void doSomething() {
        synchronized (setView) {  // Incorrectly synchronizes on setView, bug should be detected at this line
            for (Integer k : setView) {
                System.out.println(k);
            }
        }
    }
}
