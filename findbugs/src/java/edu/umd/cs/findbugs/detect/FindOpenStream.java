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

package edu.umd.cs.findbugs.detect;

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.*;

/**
 * A Detector to look for streams that are opened in a method,
 * do not escape the method, and are not closed on all paths
 * out of the method.
 */
public class FindOpenStream extends ResourceTrackingDetector<Stream, StreamResourceTracker>  {
	static final boolean DEBUG = Boolean.getBoolean("fos.debug");
	static final boolean IGNORE_WRAPPED_UNINTERESTING_STREAMS = !Boolean.getBoolean("fos.allowWUS");

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static class PotentialOpenStream {
		public final String bugType;
		public final int priority;
		public final Stream stream;

		public PotentialOpenStream(String bugType, int priority, Stream stream) {
			this.bugType = bugType;
			this.priority = priority;
			this.stream = stream;
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private List<PotentialOpenStream> potentialOpenStreamList;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindOpenStream(BugReporter bugReporter) {
		super(bugReporter);
		this.potentialOpenStreamList = new LinkedList<PotentialOpenStream>();
	}

	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet.get(Constants.NEW);
	}

	public StreamResourceTracker getResourceTracker(ClassContext classContext, Method method) {
		return new StreamResourceTracker(bugReporter);
	}

	public static boolean isMainMethod(Method method) {
		return  method.isStatic()
			&& method.getName().equals("main")
			&& method.getSignature().equals("([Ljava/lang/String;)V");
		}
		
	public void analyzeMethod(ClassContext classContext, Method method,
		StreamResourceTracker resourceTracker)
		throws CFGBuilderException, DataflowAnalysisException {

		if (isMainMethod(method)) return;

		potentialOpenStreamList.clear();

		super.analyzeMethod(classContext, method, resourceTracker);

		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);

		resourceTracker.markTransitiveUninterestingStreamEscapes();

		Iterator<PotentialOpenStream> i = potentialOpenStreamList.iterator();
		while (i.hasNext()) {
			PotentialOpenStream pos = i.next();

			Stream stream = pos.stream;

			if (stream.isUninteresting())
				continue;

			InstructionHandle constructionHandle = stream.getConstructorHandle();
			if (constructionHandle == null)
				continue;

			if (IGNORE_WRAPPED_UNINTERESTING_STREAMS
				&& resourceTracker.isUninterestingStreamEscape(constructionHandle))
				continue;

			String sourceFile = javaClass.getSourceFileName();
			bugReporter.reportBug(new BugInstance(pos.bugType, pos.priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, stream.getLocation().getHandle())
			);
		}
	}

	public void inspectResult(JavaClass javaClass, MethodGen methodGen, CFG cfg,
		Dataflow<ResourceValueFrame, ResourceValueAnalysis<Stream>> dataflow, Stream stream) {

		ResourceValueFrame exitFrame = dataflow.getResultFact(cfg.getExit());

		int exitStatus = exitFrame.getStatus();
		if (exitStatus == ResourceValueFrame.OPEN
			|| exitStatus == ResourceValueFrame.OPEN_ON_EXCEPTION_PATH) {
			String bugType;
			int priority;
			if (exitStatus == ResourceValueFrame.OPEN) {
				bugType = "OS_OPEN_STREAM";
				priority = NORMAL_PRIORITY;
			} else {
				bugType = "OS_OPEN_STREAM_EXCEPTION_PATH";
				priority = LOW_PRIORITY;
			}
			potentialOpenStreamList.add(new PotentialOpenStream(bugType, priority, stream));
		}
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + FindOpenStream.class.getName() +
				" <class file> <method name> <bytecode offset>");
			System.exit(1);
		}

		String classFile = argv[0];
		String methodName = argv[1];
		int offset = Integer.parseInt(argv[2]);

		ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker> driver =
			new ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker>() {
			public StreamResourceTracker createResourceTracker(ClassContext classContext, Method method) {
				return new StreamResourceTracker(classContext.getLookupFailureCallback());
			}
		};

		driver.execute(classFile, methodName, offset);
	}

}

// vim:ts=3
