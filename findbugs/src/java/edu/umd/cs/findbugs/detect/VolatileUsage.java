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

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class VolatileUsage extends PreorderVisitor implements Detector, 
		Constants2 {
	  private BugReporter bugReporter;
	private AnalysisContext analysisContext;

        public VolatileUsage(BugReporter bugReporter) {
                this.bugReporter = bugReporter;
        }
      public void visitClassContext(ClassContext classContext) {
                classContext.getJavaClass().accept(this);
        }

        public void setAnalysisContext(AnalysisContext analysisContext) {
                this.analysisContext = analysisContext;
        }

	public void visit(Field obj) {
		super.visit(obj);
		int flags = obj.getAccessFlags();
		if ((flags & ACC_VOLATILE) == 0) return;
		if (getFieldSig().charAt(0) == '[')
		   bugReporter.reportBug(
			new BugInstance("VO_VOLATILE_REFERENCE_TO_ARRAY", 
					NORMAL_PRIORITY)
				.addClass(this)
		.addField(FieldAnnotation.fromVisitedField(this)));
	}

	public void report() {
		}
}
