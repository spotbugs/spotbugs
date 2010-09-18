/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Grouper.java
 *
 * Created on April 5, 2003, 3:46 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Given a sorted Collection and a Comparator, produces groups of objects
 * that compare as equal.  If the Collection is not sorted, this
 * class will not work correctly.
 *
 * @author David Hovemeyer
 */
public class Grouper <ElementType> {

    public interface Callback <ElementType2> {
        public void startGroup(ElementType2 firstMember);

        public void addToGroup(ElementType2 member);
    }

    private Callback<ElementType> callback;

    /**
     * Creates a new instance of Grouper.
     *
	 * @param callback the callback which receives the groups and elements
     */
    public Grouper(Callback<ElementType> callback) {
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
    public void group(Collection<ElementType> collection, Comparator<ElementType> comparator) {
        Iterator<ElementType> i = collection.iterator();
        ElementType last = null;
		while (i.hasNext()) {
            ElementType current = i.next();
            if (last != null && comparator.compare(last, current) == 0) {
                // Same group as before
				callback.addToGroup(current);
            } else {
                // Start of a new group
                callback.startGroup(current);
			}

            last = current;
        }
    }

}
