/*
 * Grouper.java
 *
 * Created on April 5, 2003, 3:46 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.*;

/**
 * Given a sorted Collection and a Comparator, produces groups of objects
 * that compare as equal.  If the Collection is not sorted, this
 * class will not work correctly.
 *
 * @author David Hovemeyer
 */
public class Grouper {
    
    public interface Callback {
	public void startGroup(Object firstMember);
	public void addToGroup(Object member);
    }
    
    private Callback callback;
    
    /**
     * Creates a new instance of Grouper.
     * @param callback the callback which receives the groups and elements
     */
    public Grouper(Callback callback) {
	this.callback = callback;
    }

    /**
     * Group elements of given collection according to given
     * compartor's test for equality.  The groups are specified by
     * calls to the Grouper's callback object.
     *
     * @param collection the collection
     * @param comparator the comparator
     */
    public void group(Collection collection, Comparator comparator) {
	Iterator i = collection.iterator();
	Object last = null;
	while (i.hasNext()) {
	    Object current = i.next();
	    if (last != null && comparator.compare(last, current) == 0) {
		// Start of a new group
		callback.startGroup(current);
	    } else {
		// Same group as before
		callback.addToGroup(current);
	    }
	    
	    last = current;
	}
    }
    
}
