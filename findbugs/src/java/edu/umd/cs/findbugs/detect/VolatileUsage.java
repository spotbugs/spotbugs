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

public class VolatileUsage extends BytecodeScanningDetector implements 
		Constants2 {
	  private BugReporter bugReporter;
	//private AnalysisContext analysisContext;

        public VolatileUsage(BugReporter bugReporter) {
                this.bugReporter = bugReporter;
        }
      public void visitClassContext(ClassContext classContext) {
                classContext.getJavaClass().accept(this);
        }

        public void setAnalysisContext(AnalysisContext analysisContext) {
                //this.analysisContext = analysisContext;
        }

static class FieldRecord {
                String className;
                String name;
                String signature;
                boolean isStatic;
        }


	HashMap<String,FieldRecord> fieldInfo = new HashMap<String,FieldRecord>();
	HashSet<String> initializationWrites = new HashSet<String>();
	HashSet<String> otherWrites = new HashSet<String>();
	

	public void visit(Field obj) {
		super.visit(obj);
		int flags = obj.getAccessFlags();
		if ((flags & ACC_VOLATILE) == 0) return;
		if (getFieldSig().charAt(0) == '[') {
		   FieldRecord f = new FieldRecord();
			f.className = getDottedClassName();
			f.name = getFieldName();
			f.signature = getDottedFieldSig();
			f.isStatic = !((flags & ACC_STATIC) == 0);
			fieldInfo.put(getDottedClassName() + "." + getFieldName(),
					f);
		}
	}

    public void sawOpcode(int seen) {
                switch (seen) {
                case PUTSTATIC:
			{
                        String name = (getClassConstantOperand() 
					+ "." + getNameConstantOperand())
                                .replace('/', '.');
			if (getMethodName().equals("<clinit>"))
				initializationWrites.add(name);
			else otherWrites.add(name);
			break;
			}
                case PUTFIELD:
                        {
			String name = (getClassConstantOperand() 
					+ "." + getNameConstantOperand())
                                .replace('/', '.');
			if (getMethodName().equals("<init>"))
				initializationWrites.add(name);
			else otherWrites.add(name);
			break;
			}
		}
		}
				

	public void report() {

		for(Map.Entry<String, FieldRecord> r : fieldInfo.entrySet()) {	
		   String name = r.getKey();
		   FieldRecord f = r.getValue();
		   int priority = LOW_PRIORITY;
		   if (initializationWrites.contains(name) 
			&& !otherWrites.contains(name))
		     priority = NORMAL_PRIORITY;
		   bugReporter.reportBug(
			new BugInstance(this, "VO_VOLATILE_REFERENCE_TO_ARRAY", priority)
			 .addClass(f.className)
			 .addField(f.className, f.name, f.signature, f.isStatic));
		}
		}
}
