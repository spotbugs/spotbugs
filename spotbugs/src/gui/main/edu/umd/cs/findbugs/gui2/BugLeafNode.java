/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.gui2.BugAspects.SortableValue;

/**
 * These are the leaves of the tree, note that coloring is not done here, it is
 * done in BugRenderer This class is basically just a wrapper for BugInstance
 */

// only thing of note is the equals method, which purposefully uses == since
// JTree's only show one of multiple equal objects.

public class BugLeafNode {

    private final BugInstance bug;

    BugLeafNode(BugInstance b) {
        bug = b;
    }

    public BugInstance getBug() {
        return bug;
    }

    @Override
    public String toString() {
        if (bug.isDead()) {
            return "\u2620 " + bug.getMessageWithoutPrefix();
        }
        return bug.getMessageWithoutPrefix();
    }

    public boolean matches(SortableValue keyValuePair) {
        /*
         * try { Method m = BugInstance.class.getMethod("get" +
         * keyValuePair.key,new Class[0]); return
         * (keyValuePair.value.equals(m.invoke(this,new Object[0]))); }
         * catch(SecurityException e) {
         * System.err.println("NoOoOOOooOoOo000!!!1!!!1one!"); } catch
         * (NoSuchMethodException e) { throw new IllegalArgumentException("get"
         * + keyValuePair.key + " does not exist"); } catch
         * (IllegalArgumentException e) { e.printStackTrace(); } catch
         * (IllegalAccessException e) { System.err.println("Make the method get"
         * + keyValuePair.key +
         * " public or package or ... something.  ..  Now.");
         * e.printStackTrace(); } catch (InvocationTargetException e) {
         * e.printStackTrace(); }
         */
        return keyValuePair.key.getFrom(bug).equals(keyValuePair.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BugLeafNode)) {
            return false;
        } else {
            return bug == (((BugLeafNode) o).getBug());
        }
    }

    @Override
    public int hashCode() {
        return bug.hashCode();
    }

    public boolean matches(BugAspects aspects) {
        if (aspects.size() == 0) {
            return true;
        }
        for (BugAspects.SortableValue strPair : aspects) {
            if (!matches(strPair)) {
                return false;
            }
        }

        return true;
    }
}
