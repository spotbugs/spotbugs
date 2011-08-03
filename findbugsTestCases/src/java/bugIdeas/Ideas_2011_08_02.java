package bugIdeas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.Priority;

public class Ideas_2011_08_02 {

    @ExpectWarning("INT_BAD_COMPARISON_WITH_INT_VALUE")
    public boolean badCheck(int x) {
        return x == 9999999999L;
    }

    @ExpectWarning("INT_BAD_COMPARISON_WITH_INT_VALUE")
    public boolean badCheck2(int x) {
        return 9999999999L == x;
    }

    @NoWarning("INT_BAD_COMPARISON_WITH_INT_VALUE")
    public boolean okCheck(int x) {
        return x == 999999999L;
    }

    enum Color {
        RED, GREEN, BLUE
    };

    Collection<Map.Entry<Color, Integer>> c = new ArrayList<Map.Entry<Color, Integer>>();

    @NoWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.HIGH)
    @ExpectWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.MEDIUM)
    public void addAll(Map<Color, Integer> m) {
        c.addAll(m.entrySet());
    }

    @ExpectWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.HIGH) 
    public void addAll(IdentityHashMap<Color, Integer> m) {
        c.addAll(m.entrySet());
    }
    @ExpectWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.HIGH)
    
    public void addAll(EnumMap<Color, Integer> m) {
        c.addAll(m.entrySet());
    }
    @ExpectWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.MEDIUM) 
    public void addAll(ConcurrentHashMap<Color, Integer> m) {
        c.addAll(m.entrySet());
    }
    @ExpectWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.MEDIUM) 
    public void addAll(ConcurrentSkipListMap<Color, Integer> m) {
        c.addAll(m.entrySet());
    }

    @NoWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.MEDIUM)
    public void addAll(HashMap<Color, Integer> m) {
        c.addAll(m.entrySet());
    }
    @NoWarning(value="DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority=Priority.MEDIUM)
    public void addAll(TreeMap<Color, Integer> m) {
        c.addAll(m.entrySet());
    }
    
    static abstract class BadIdea implements Map.Entry<String, Integer>, Iterator<Map.Entry<String, Integer>> {
        
    }

}