package edu.umd.cs.findbugs;

import edu.umd.cs.pugh.visitclass.DismantleBytecode;

/**
 * Base class for Detectors which want to extend DismantleBytecode.
 * @see DismantleBytecode
 */
public class BytecodeScanningDetector extends DismantleBytecode implements Detector {
  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
  }

  public void report() { }
}
