package sfBugs;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Submitted By: Ulrich Radermacher Summary:
 * 
 * WMI, WrongMapIterator states: It is not efficient to ioterate over the
 * keyset, and use the key than to get the value assigned to this key.
 * 
 * Here I found 2 kinds of false positives:
 * 
 * 1) If the Map is a SortedMap, it seems to me perfectly legal to iterate over
 * the keyset to take advantage of the sorting.
 * 
 * 2) In one case I have to do something with the key (it is passed to another
 * method as parameter) and just in a log statement if have: log("Handled " +
 * key + " desription = " + map.get(key)); So if the key is used for more than
 * just get the value is is legal to do so even for unsorted maps.
 */
public class Bug1751003 {

    public static void main(String args[]) {
        Bug1751003 b = new Bug1751003();
        b.sortedMapIterate();
        b.useKeyAndValIterate();
    }

    // // grep -A 1 WMI_WRONG_MAP_ITERATOR | grep Bug1751003
    public void sortedMapIterate() {
        SortedMap<Integer, Integer> sm = new TreeMap<Integer, Integer>();
        sm.put(1, 2);
        sm.put(3, 4);
        sm.put(5, 6);
        for (Integer I : sm.keySet()) {
            Integer J = sm.get(I);
            if (J > 10) {
                System.out.println("");
            }
        }
    }

    // // grep -A 1 WMI_WRONG_MAP_ITERATOR | grep Bug1751003
    public void useKeyAndValIterate() {
        HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
        hm.put(1, 2);
        hm.put(3, 4);
        hm.put(5, 6);
        for (Integer I : hm.keySet()) {
            Integer J = hm.get(I);
            if (J > 10 && I > 20) {
                System.out.println("");
            }
        }
    }
}
