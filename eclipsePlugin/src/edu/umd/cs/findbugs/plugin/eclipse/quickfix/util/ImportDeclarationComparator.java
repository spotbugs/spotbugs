/*
 * Contributions to FindBugs
 * Copyright (C) 2007, Institut for Software
 * An Institut of the University of Applied Sciences Rapperswil
 * 
 * Author: Thierry Wyss, Marco Busarello
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix.util;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

import java.io.Serializable;
import java.util.Comparator;

import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;

/**
 * A <CODE>Comparator</CODE> used to add imports in a sorted order. The <CODE>ImportDeclaration</CODE>
 * will be sorted according to static or not static import, and then in an
 * alphabetically order.
 * 
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class ImportDeclarationComparator<E extends ImportDeclaration> implements Comparator<E>, Serializable {

    public int compare(E o1, E o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return MAX_VALUE;
        }
        if (o2 == null) {
            return MIN_VALUE;
        }

        if (!(o1.isStatic() ^ o2.isStatic())) {
            return compare(o1.getName(), o2.getName());
        }

        return o1.isStatic() ? MIN_VALUE : MAX_VALUE;
    }

    private int compare(Name o1, Name o2) {
        return o1.getFullyQualifiedName().compareTo(o2.getFullyQualifiedName());
    }

}
