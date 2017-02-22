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

package edu.umd.cs.findbugs;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.graph.AbstractEdge;

public class CallGraphEdge extends AbstractEdge<CallGraphEdge, CallGraphNode> {
    private CallSite callSite;

    CallGraphEdge(CallGraphNode source, CallGraphNode target) {
        super(source, target);
    }

    void setCallSite(CallSite callSite) {
        this.callSite = callSite;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public Method getCallingMethod() {
        return getSource().getMethod();
    }

    public Method getCalledMethod() {
        return getTarget().getMethod();
    }
}

