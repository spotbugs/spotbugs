/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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
package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.InterproceduralCallGraph;
import edu.umd.cs.findbugs.ba.ch.InterproceduralCallGraphVertex;
import edu.umd.cs.findbugs.ba.jsr305.Analysis;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Build the interprocedural call graph.
 *
 * NOTE: at the present time, this facility is only used to find relevant type
 * qualifiers. It could become a more general-purpose facility if there were a
 * need.
 *
 * @author David Hovemeyer
 */
public class BuildInterproceduralCallGraph extends BytecodeScanningDetector implements NonReportingDetector {

    private final InterproceduralCallGraph callGraph;

    private InterproceduralCallGraphVertex currentVertex;

    /**
     * Constructor.
     *
     * @param bugReporter
     *            the BugReporter to use
     */
    public BuildInterproceduralCallGraph(BugReporter bugReporter) {
        if (!Analysis.FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
            return;
        }
        callGraph = new InterproceduralCallGraph();
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (!Analysis.FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
            return;
        }
        super.visitClassContext(classContext);
    }

    @Override
    public void visitMethod(Method obj) {
        currentVertex = findVertex(getXMethod());
        super.visitMethod(obj);
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case Constants.INVOKESTATIC:
        case Constants.INVOKEVIRTUAL:
        case Constants.INVOKEINTERFACE:
        case Constants.INVOKESPECIAL:
            MethodDescriptor called = getMethodDescriptorOperand();
            XMethod calledXMethod = XFactory.createXMethod(called);
            InterproceduralCallGraphVertex calledVertex = findVertex(calledXMethod);
            callGraph.createEdge(currentVertex, calledVertex);
            break;
        default:
            break;
        }
    }

    /**
     * Find the InterproceduralCallGraphVertex for given XMethod.
     *
     * @param xmethod
     *            an XMethod
     * @return the XMethod's InterproceduralCallGraphVertex
     */
    private InterproceduralCallGraphVertex findVertex(XMethod xmethod) {
        InterproceduralCallGraphVertex vertex;
        vertex = callGraph.lookupVertex(xmethod.getMethodDescriptor());
        if (vertex == null) {
            vertex = new InterproceduralCallGraphVertex();
            vertex.setXmethod(xmethod);
            callGraph.addVertex(vertex);
        }
        return vertex;
    }

    @Override
    public void report() {
        if (!Analysis.FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
            return;
        }
        Global.getAnalysisCache().eagerlyPutDatabase(InterproceduralCallGraph.class, callGraph);
    }
}
