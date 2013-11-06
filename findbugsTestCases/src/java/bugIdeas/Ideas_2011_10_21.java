package bugIdeas;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.detect.FindUnrelatedTypesInGenericContainer;

@DetectorUnderTest(FindUnrelatedTypesInGenericContainer.class)
public class Ideas_2011_10_21 {

    Hashtable<String, Integer> hashtable = new Hashtable<String, Integer>();

    ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<String, Integer>();

    @NoWarning("GC_UNRELATED_TYPES")
    public boolean testContainsHashtable(Integer i) {
        return hashtable.contains(i);
    }

    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean testContainsHashtable(String s) {
        return hashtable.contains(s);
    }

    @NoWarning("GC_UNRELATED_TYPES")
    public boolean testContainsConcurrentHashMap(Integer i) {
        return concurrentHashMap.contains(i);
    }

    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean testContainsConcurrentHashMap(String s) {
        return concurrentHashMap.contains(s);
    }

    @NoWarning("GC_UNRELATED_TYPES")
    public boolean testContainsValueHashtable(Integer i) {
        return hashtable.containsValue(i);
    }

    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean testContainsValueHashtable(String s) {
        return hashtable.containsValue(s);
    }

    @NoWarning("GC_UNRELATED_TYPES")
    public boolean testContainsValueConcurrentHashMap(Integer i) {
        return concurrentHashMap.containsValue(i);
    }

    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean testContainsValueConcurrentHashMap(String s) {
        return concurrentHashMap.containsValue(s);
    }

    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean testRemove(String s) {
        return concurrentHashMap.remove(s, s);
    }

    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean testRemove(Integer i) {
        return concurrentHashMap.remove(i, i);
    }

    @NoWarning("GC_UNRELATED_TYPES")
    public boolean testRemove(String s, Integer i) {
        return concurrentHashMap.remove(s, i);
    }

    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean testRemove(Integer i, String s) {
        return concurrentHashMap.remove(i, s);
    }

}
