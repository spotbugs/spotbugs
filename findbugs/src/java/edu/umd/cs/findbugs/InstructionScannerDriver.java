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

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.Edge;

/**
 * Drive an InstructionScannerGenerator over the instructions of a simple path.
 * The generator will create scanners at certain instructions. Each instruction
 * and edge is fed to all scanners so created.
 */
public class InstructionScannerDriver {
    private final Iterator<Edge> edgeIter;

    private final LinkedList<InstructionScanner> scannerList;

    private static final boolean DEBUG = SystemProperties.getBoolean("isd.debug");

    /**
     * Constructor.
     *
     * @param edgeIter
     *            iterator over Edges specifying path to be scanned
     */
    public InstructionScannerDriver(Iterator<Edge> edgeIter) {
        this.edgeIter = edgeIter;
        scannerList = new LinkedList<InstructionScanner>();
    }

    /**
     * Execute by driving the InstructionScannerGenerator over all instructions.
     * Each generated InstructionScanner is driven over all instructions and
     * edges.
     *
     * @param generator
     *            the InstructionScannerGenerator
     */
    public void execute(InstructionScannerGenerator generator) {
        // Pump the instructions in the path through the generator and all
        // generated scanners
        while (edgeIter.hasNext()) {
            Edge edge = edgeIter.next();
            BasicBlock source = edge.getSource();
            if (DEBUG) {
                System.out.println("ISD: scanning instructions in block " + source.getLabel());
            }

            // Traverse all instructions in the source block
            Iterator<InstructionHandle> i = source.instructionIterator();
            int count = 0;
            while (i.hasNext()) {
                InstructionHandle handle = i.next();

                // Check if the generator wants to create a new scanner
                if (generator.start(handle)) {
                    scannerList.add(generator.createScanner());
                }

                // Pump the instruction into all scanners
                for (InstructionScanner scanner : scannerList) {
                    scanner.scanInstruction(handle);
                }

                ++count;
            }

            if (DEBUG) {
                System.out.println("ISD: scanned " + count + " instructions");
            }

            // Now that we've finished the source block, pump the edge
            // into all scanners
            for (InstructionScanner scanner : scannerList) {
                scanner.traverseEdge(edge);
            }
        }
    }
}

