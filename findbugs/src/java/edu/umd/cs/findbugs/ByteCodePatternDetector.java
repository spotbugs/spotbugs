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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePattern;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePatternMatch;
import edu.umd.cs.findbugs.ba.bcp.PatternElementMatch;
import edu.umd.cs.findbugs.ba.bcp.PatternMatcher;

/**
 * A base class for bug detectors that are based on a ByteCodePattern.
 * ByteCodePatterns provide an easy way to detect patterns of
 * bytecode instructions, taking into account control flow and
 * uses of fields and values.
 *
 * @see ByteCodePattern
 */
public abstract class ByteCodePatternDetector implements Detector {
	private static final boolean DEBUG = SystemProperties.getBoolean("bcpd.debug");
	private static final String METHOD = SystemProperties.getProperty("bcpd.method");

	protected abstract BugReporter getBugReporter();

	public void visitClassContext(ClassContext classContext) {
		try {
			ByteCodePattern pattern = getPattern();
			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();

			for (Method method : methodList) {
				if (method.isAbstract() || method.isNative())
					continue;

				if (METHOD != null && !method.getName().equals(METHOD))
					continue;

				if (DEBUG) {
					System.out.print("=====================================================================\n" +
							"Method " + jclass.getClassName() + "." + method.getName() + "\n" +
							"=====================================================================\n");
				}

				if (!prescreen(method, classContext))
					continue;

				MethodGen methodGen = classContext.getMethodGen(method);
				if (methodGen == null)
					continue;

				PatternMatcher matcher = new PatternMatcher(pattern, classContext, method);
				matcher.execute();

				Iterator<ByteCodePatternMatch> j = matcher.byteCodePatternMatchIterator();
				while (j.hasNext()) {
					ByteCodePatternMatch match = j.next();

					if (DEBUG) {
						System.out.println("Pattern match:");
						Iterator<PatternElementMatch> pemIter = match.patternElementMatchIterator();
						while (pemIter.hasNext()) {
							PatternElementMatch pem = pemIter.next();
							System.out.println("\t" + pem.toString());
						}
					}

					reportMatch(classContext, method, match);
				}
			}
		} catch (DataflowAnalysisException e) {
			getBugReporter().logError(getDetectorName() + " caught exception", e);
		} catch (CFGBuilderException e) {
			getBugReporter().logError(getDetectorName() + " caught exception", e);
		}
	}

	private String getDetectorName() {
		String className = this.getClass().getName();
		int lastDot = className.lastIndexOf('.');
		if (lastDot >= 0) {
			className = className.substring(lastDot + 1);
		}
		return className;
	}

	public void report() {
	}

	/**
	 * Get the ByteCodePattern for this detector.
	 */
	public abstract ByteCodePattern getPattern();

	/**
	 * Prescreen a method.
	 * It is a valid, but dumb, implementation simply to return true unconditionally.
	 * A better implementation is to call ClassContext.getBytecodeSet() to check
	 * whether the method actually contains the bytecode instructions that
	 * the pattern will look for.  The theory is that checking the bytecode
	 * set is very fast, while building the MethodGen, CFG, ValueNumberAnalysis,
	 * etc. objects required to match ByteCodePatterns is slow, and the bytecode
	 * pattern matching algorithm is also not particularly fast.
	 * <p/>
	 * <p> As a datapoint, prescreening speeds up the BCPDoubleCheck detector
	 * <b>by a factor of 5</b> with no loss of generality and only a dozen
	 * or so extra lines of code.
	 *
	 * @param method       the method
	 * @param classContext the ClassContext for the method
	 * @return true if the method should be analyzed for instances of the
	 *         ByteCodePattern
	 */
	public abstract boolean prescreen(Method method, ClassContext classContext);

	/**
	 * Called to report an instance of the ByteCodePattern.
	 *
	 * @param classContext the ClassContext for the analyzed class
	 * @param method       the method to instance appears in
	 * @param match        the ByteCodePatternMatch object representing the match
	 *                     of the ByteCodePattern against actual instructions in the method
	 */
	public abstract void reportMatch(ClassContext classContext, Method method, ByteCodePatternMatch match)
			throws CFGBuilderException, DataflowAnalysisException;
}

// vim:ts=4
