/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.view.explorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

public class Grouping {

    private final LinkedList<GroupType> groupOrder;

    private Grouping(List<GroupType> types) {
        groupOrder = new LinkedList<GroupType>(types);
        // at least marker should be shown
        if (!groupOrder.contains(GroupType.Marker)) {
            groupOrder.add(GroupType.Marker);
        }
    }

    private static Grouping createDefault() {
        List<GroupType> order = new ArrayList<GroupType>();
        order.add(GroupType.Project);
        order.add(GroupType.BugRank);
        order.add(GroupType.Confidence);
        order.add(GroupType.Pattern);
        order.add(GroupType.Marker);
        return createFrom(order);
    }

    @Nonnull
    public static Grouping createFrom(List<GroupType> types) {
        return new Grouping(types);
    }

    @Nonnull
    public List<GroupType> asList() {
        return new LinkedList<GroupType>(groupOrder);
    }

    @Nonnull
    public GroupType getFirstType() {
        return groupOrder.size() > 0 ? groupOrder.getFirst() : GroupType.Undefined;
    }

    @Nonnull
    public GroupType getChildType(GroupType parent) {
        if (parent == GroupType.Marker) {
            return parent;
        }
        for (int i = 0; i < groupOrder.size(); i++) {
            if (groupOrder.get(i) == parent) {
                return i + 1 < groupOrder.size() ? groupOrder.get(i + 1) : GroupType.Marker;
            }
        }
        return GroupType.Marker;
    }

    Iterator<GroupType> iterator() {
        return groupOrder.iterator();
    }

    public GroupType getParentType(GroupType child) {
        for (int i = 0; i < groupOrder.size(); i++) {
            if (groupOrder.get(i) == child) {
                return i - 1 >= 0 ? groupOrder.get(i - 1) : GroupType.Undefined;
            }
        }
        return GroupType.Undefined;
    }

    static Grouping restoreFrom(String saved) {
        if (saved == null || saved.length() == 0) {
            return createDefault();
        }
        StringTokenizer st = new StringTokenizer(saved, "[] ,", false);
        List<GroupType> types = new ArrayList<GroupType>();
        while (st.hasMoreTokens()) {
            GroupType type = GroupType.getType(st.nextToken());
            types.add(type);
        }
        if (types.isEmpty()) {
            return createDefault();
        }
        return createFrom(types);
    }

    public boolean contains(GroupType type) {
        return groupOrder.contains(type);
    }

    int compare(GroupType g1, GroupType g2) {
        return groupOrder.indexOf(g1) - groupOrder.indexOf(g2);
    }

    @Override
    public String toString() {
        return groupOrder.toString();
    }

}
