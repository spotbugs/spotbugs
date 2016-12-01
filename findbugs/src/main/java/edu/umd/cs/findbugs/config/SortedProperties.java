package edu.umd.cs.findbugs.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author pugh
 */
public final class SortedProperties extends Properties {
    /**
     * Overriden to be able to write properties sorted by keys to the disk
     *
     * @see java.util.Hashtable#keys()
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized Enumeration<Object> keys() {
        // sort elements based on detector (prop key) names
        Set<?> set = keySet();
        return (Enumeration<Object>) sortKeys((Set<String>) set);
    }

    /**
     * To be compatible with version control systems, we need to sort properties
     * before storing them to disk. Otherwise each change may lead to problems
     * by diff against previous version - because Property entries are randomly
     * distributed (it's a map).
     *
     * @param keySet
     *            non null set instance to sort
     * @return non null list which contains all given keys, sorted
     *         lexicographically. The list may be empty if given set was empty
     */
    static public Enumeration<?> sortKeys(Set<String> keySet) {
        List<String> sortedList = new ArrayList<String>();
        sortedList.addAll(keySet);
        Collections.sort(sortedList);
        return Collections.enumeration(sortedList);
    }
}
