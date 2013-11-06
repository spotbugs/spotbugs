package bugIdeas;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2009_10_13a {

    @ExpectWarning("GC")
    public static void truePositive3(HashMap<Integer, String> i, HashMap<String, Integer> h) {
        if (i.entrySet().containsAll(h.entrySet()))
            System.out.println("i contains h");
        if (h.entrySet().containsAll(i.entrySet()))
            System.out.println("h contains i");
        h.entrySet().retainAll(i.entrySet());
        h.entrySet().removeAll(i.entrySet());
    }
    
    @ExpectWarning("GC")
    public static void truePositive4(HashMap<Integer, String> i, HashMap<String, Integer> h) {
        Set<Entry<Integer, String>> iEntrySet = i.entrySet();
        Set<Entry<String, Integer>> hEntrySet = h.entrySet();
        if (iEntrySet.containsAll(hEntrySet))
            System.out.println("i contains h");
        if (hEntrySet.containsAll(iEntrySet))
            System.out.println("h contains i");
        hEntrySet.retainAll(iEntrySet);
        hEntrySet.removeAll(iEntrySet);
    }


}
