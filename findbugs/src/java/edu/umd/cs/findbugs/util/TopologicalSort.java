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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;

/**
 * @author pugh
 */
public class TopologicalSort {
    
    public interface OutEdges<E> {
        Collection<E> getOutEdges(E e);
    }
    
    public static <E> List<E> sortByCallGraph(Collection<E> elements, OutEdges<E> outEdges) {
        List<E> result = new ArrayList<E>(elements.size());
        Foo<E> instance = new Foo<E>(result, outEdges);
        instance.consider.addAll(elements);
        for (E e : elements)
            instance.visit(e);
        return result;
    }

    static class Foo<E> {
        Foo(List<E> result, OutEdges<E> outEdges) {
            this.result = result;
            this.outEdges = outEdges;

        }
        OutEdges<E> outEdges;
        
        List<E> result;

        HashSet<E> visited = new HashSet<E>();
        HashSet<E> consider = new HashSet<E>();

        void visit(E e) {
            if (!consider.contains(e)) return;
            if (!visited.add(e))
                return;
             for(E e2  :outEdges.getOutEdges(e))
                visit(e2);
           
            result.add(e);
        }
    }

}
