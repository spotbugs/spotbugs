package edu.umd.cs.findbugs;

/**
 * Class for generating InstructionScanners at particular instructions of a path.
 * Because we don't know a priori where we might want to start scanning
 * in order to find a pattern in a path, we use this interface to tell us which
 * locations are candidates for starting a pattern.
 */
public interface InstructionScannerGenerator {
	/**
	 * Return true if a new scanner should be created starting at this instruction,
	 * false otherwise.
	 */
	public boolean start(org.apache.bcel.generic.Instruction ins);
	/**
	 * Create a new scanner.
	 */
	public InstructionScanner createScanner();
}

// vim:ts=4
