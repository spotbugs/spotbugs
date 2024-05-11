package synchronizationLocks.collectionViews;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class GoodSynchronizationOnCollectionView {
    private final Map<Integer, String> mapView = Collections.synchronizedMap(new HashMap<Integer, String>());
    private final Set<Integer> setView = mapView.keySet();

    public Map<Integer, String> getMap() {
        return mapView;
    }

    public void doSomething() {
        synchronized (mapView) {  // Synchronize on map, rather than set
            for (Integer k : setView) {
                System.out.println(k);
            }
        }
    }
}
