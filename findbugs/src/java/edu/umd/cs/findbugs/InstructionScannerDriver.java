package edu.umd.cs.findbugs;

import java.util.*;
import java.io.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

/**
 * Drive an InstructionScannerGenerator over the instructions of
 * a simple path.  The generator will create scanners at certain instructions.
 * Each instruction and edge is fed to all scanners so created.
 */
public class InstructionScannerDriver {
	private Iterator<Edge> edgeIter;
	private LinkedList<InstructionScanner> scannerList;

	private static final boolean DEBUG = Boolean.getBoolean("isd.debug");

	/**
	 * Constructor.
	 * @param edgeIter iterator over Edges specifying path to be scanned
	 */
	public InstructionScannerDriver(Iterator<Edge> edgeIter) {
		this.edgeIter = edgeIter;
		scannerList = new LinkedList<InstructionScanner>();
	}

	/**
	 * Execute by driving the InstructionScannerGenerator over all instructions.
	 * Each generated InstructionScanner is driven over all instructions and
	 * edges.
	 * @param generator the InstructionScannerGenerator
	 */
	public void execute(InstructionScannerGenerator generator) {
		// Pump the instructions in the path through the generator and all generated scanners
		while (edgeIter.hasNext()) {
			Edge edge = edgeIter.next();
			BasicBlock source = edge.getSource();
			if (DEBUG) System.out.println("ISD: scanning instructions in block " + source.getId());

			// Traverse all instructions in the source block
			Iterator<InstructionHandle> i = source.instructionIterator();
			int count = 0;
			while (i.hasNext()) {
				Instruction ins = i.next().getInstruction();

				// Check if the generator wants to create a new scanner
				if (generator.start(ins))
					scannerList.add(generator.createScanner());

				// Pump the instruction into all scanners
				for (Iterator<InstructionScanner> j = scannerList.iterator(); j.hasNext(); ) {
					InstructionScanner scanner = j.next();
					scanner.scanInstruction(ins);
				}

				++count;
			}

			if (DEBUG) System.out.println("ISD: scanned " + count + " instructions");

			// Now that we've finished the source block, pump the edge
			// into all scanners
			for (Iterator<InstructionScanner> j = scannerList.iterator(); j.hasNext(); ) {
				InstructionScanner scanner = j.next();
				scanner.traverseEdge(edge);
			}
		}
	}
}

// vim:ts=4
