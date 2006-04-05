/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.classfile.*;

/**
 * finds public classes that use 'this' as a semaphore, which can cause conflicts if clients of this
 * class use an instance of this class as their own synchronization point. Frankly, Just calling
 * synchronized on this, or defining synchronized methods is bad, but since that is so prevalent, 
 * don't warn on that.
 */
public class PublicSemaphores extends BytecodeScanningDetector implements StatelessDetector
{
	private static final int SEEN_NOTHING = 0;
	private static final int SEEN_ALOAD_0 = 1;

	private BugReporter bugReporter;
	private int state;
	private boolean alreadyReported;
	
	public PublicSemaphores(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	


	@Override
         public void visitClassContext(ClassContext classContext) {
		JavaClass cls = classContext.getJavaClass();
		if ((!cls.isPublic()) || (cls.getClassName().indexOf("$") >= 0))
			return;
		
		alreadyReported = false;
		super.visitClassContext(classContext);
	}
	
	@Override
         public void visit(Code obj) {
		Method m = getMethod();
		if (m.isStatic() || alreadyReported)
			return;
		
		state = SEEN_NOTHING;
		super.visit(obj);
	}
	
	@Override
         public void sawOpcode(int seen) {
		if (alreadyReported)
			return;
		
		switch (state) {
			case SEEN_NOTHING:
				if (seen == ALOAD_0)
					state = SEEN_ALOAD_0;
			break;
			
			case SEEN_ALOAD_0:
				if ((seen == INVOKEVIRTUAL)
				&&  getClassConstantOperand().equals("java/lang/Object")) {
					String methodName = getNameConstantOperand();
					if ("wait".equals(methodName) || "notify".equals(methodName) || "notifyAll".equals(methodName)) {
						bugReporter.reportBug( new BugInstance( this, "PS_PUBLIC_SEMAPHORES", NORMAL_PRIORITY )
						        .addClassAndMethod(this)
						        .addSourceLine(this));
						alreadyReported = true;
					}
				}
				state = SEEN_NOTHING;
			break;
		}
		
	}
	
}
