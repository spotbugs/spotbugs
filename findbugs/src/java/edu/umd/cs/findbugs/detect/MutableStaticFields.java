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


import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;

public class MutableStaticFields extends BytecodeScanningDetector {


	static String extractPackage(String c) {
		int i = c.lastIndexOf('/');
		if (i < 0) return "";
		return c.substring(0, i);
	}

	static boolean mutableSignature(String sig) {
		return sig.equals("Ljava/util/Hashtable;")
				|| sig.equals("Ljava/util/Date;")
				|| sig.charAt(0) == '[';
	}




	LinkedList<XField> seen = new LinkedList<XField>();
	boolean publicClass;
	boolean zeroOnTOS;
	boolean emptyArrayOnTOS;
	boolean inStaticInitializer;
	String packageName;
	Set<XField> readAnywhere = new HashSet<XField>();
	Set<XField> unsafeValue = new HashSet<XField>();
	Set<XField> notFinal = new HashSet<XField>();
	Set<XField> outsidePackage = new HashSet<XField>();
	Map<XField, SourceLineAnnotation> firstFieldUse = new HashMap<XField, SourceLineAnnotation>();
	private BugReporter bugReporter;

	public MutableStaticFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
		 public void visit(JavaClass obj) {
		super.visit(obj);
		int flags = obj.getAccessFlags();
		publicClass = (flags & ACC_PUBLIC) != 0
				&& !getDottedClassName().startsWith("sun.");

		packageName = extractPackage(getClassName());
	}

	@Override
		 public void visit(Method obj) {
		zeroOnTOS = false;
		// System.out.println(methodName);
		inStaticInitializer = getMethodName().equals("<clinit>");
	}

	@Override
		 public void sawOpcode(int seen) {
		// System.out.println("saw	"	+ OPCODE_NAMES[seen] + "	" + zeroOnTOS);
		switch (seen) {
		case GETSTATIC:
		case PUTSTATIC:
			boolean samePackage =
					packageName.equals(extractPackage(getClassConstantOperand()));
			boolean initOnly =
					seen == GETSTATIC ||
					getClassName().equals(getClassConstantOperand())
					&& inStaticInitializer;
			boolean safeValue =
					seen == GETSTATIC || emptyArrayOnTOS
					|| !mutableSignature(getSigConstantOperand());
			
			XField xField = getXFieldOperand();
			if (xField == null) break;
			if (!interesting(xField)) break;
			if (seen == GETSTATIC)
				readAnywhere.add(xField);

			if (!samePackage)
				outsidePackage.add(xField);

			if (!initOnly)
				notFinal.add(xField);

			if (!safeValue)
				unsafeValue.add(xField);

			//Remove inStaticInitializer check to report all source lines of first use
			//doing so, however adds quite a bit of memory bloat.
			if (inStaticInitializer && !firstFieldUse.containsKey(xField)) {
				SourceLineAnnotation sla = SourceLineAnnotation.fromVisitedInstruction(this);
				firstFieldUse.put(xField, sla);
			}
			break;
		case ANEWARRAY:
		case NEWARRAY:
			if (zeroOnTOS)
				emptyArrayOnTOS = true;
			zeroOnTOS = false;
			return;
		case ICONST_0:
			zeroOnTOS = true;
			emptyArrayOnTOS = false;
			return;
		}
		zeroOnTOS = false;
		emptyArrayOnTOS = false;
	}

	private boolean interesting(XField f) {
		if (!f.isPublic() && !f.isProtected()) return false;
		if (!f.isStatic()|| f.isSynthetic() || f.isVolatile()) return false;
		boolean isHashtable = 
			f.getSignature().equals("Ljava/util/Hashtable;");
		boolean isArray = f.getSignature().charAt(0) == '[';
		if (f.isFinal() && !(isArray || isHashtable)) return false;
		return true;
		
	}
	@Override
		 public void visit(Field obj) {
		super.visit(obj);
		int flags = obj.getAccessFlags();
		boolean isStatic = (flags & ACC_STATIC) != 0;
		if (!isStatic) return;
		boolean isVolatile = (flags & ACC_VOLATILE) != 0;
		if (isVolatile) return;
		boolean isFinal = (flags & ACC_FINAL) != 0;
		boolean isPublic = publicClass && (flags & ACC_PUBLIC) != 0;
		boolean isProtected = publicClass && (flags & ACC_PROTECTED) != 0;
		if (!isPublic && !isProtected) return;

		boolean isHashtable = 
			getFieldSig().equals("Ljava/util/Hashtable;");
		boolean isArray = getFieldSig().charAt(0) == '[';

		if (isFinal && !(isHashtable || isArray)) return;

		seen.add(getXField());

	}

	@Override
		 public void report() {
		/*
		for(Iterator i = unsafeValue.iterator(); i.hasNext(); ) {
			System.out.println("Unsafe: " + i.next());
			}
		*/
		for (XField f : seen) {
			boolean isFinal = f.isFinal();
			String className = f.getClassName();
			String fieldSig = f.getSignature();
			String fieldName = f.getName();
			boolean couldBeFinal = !isFinal
					&& !notFinal.contains(f);
			boolean isPublic = f.isPublic();
			boolean couldBePackage = !outsidePackage.contains(f);
			boolean movedOutofInterface = false;
			
            try {
            	XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, f.getClassDescriptor());
            	movedOutofInterface = couldBePackage && xClass.isInterface();
            } catch (CheckedAnalysisException e) {
            	assert true;
            }
			boolean isHashtable = fieldSig.equals("Ljava.util.Hashtable;");
			boolean isArray = fieldSig.charAt(0) == '['
					&& unsafeValue.contains(f);
			boolean isReadAnywhere = readAnywhere.contains(f);
			if (false) 
						  System.out.println(className + "."  + fieldName
									  + " : " + fieldSig
								  + "	" + isHashtable
								  + "	" + isArray
									  );


			String bugType;
			int priority = NORMAL_PRIORITY;
			if (isFinal && !isHashtable && !isArray) {
				continue;
			} else if (movedOutofInterface) {
				bugType = "MS_OOI_PKGPROTECT";
			} else if (couldBePackage && couldBeFinal && (isHashtable || isArray))
				bugType = "MS_FINAL_PKGPROTECT";
			else if (couldBeFinal && !isHashtable && !isArray) {
				bugType = "MS_SHOULD_BE_FINAL";
				if (fieldName.equals(fieldName.toUpperCase())
						|| fieldSig.charAt(0) == 'L')
					priority = HIGH_PRIORITY;
			} else if (couldBePackage)
				bugType = "MS_PKGPROTECT";
			else if (isHashtable) {
				bugType = "MS_MUTABLE_HASHTABLE";
				if (!isFinal)
					priority = HIGH_PRIORITY;
			} else if (isArray) {
				bugType = "MS_MUTABLE_ARRAY";
				if (fieldSig.indexOf("L") >= 0 || !isFinal)
					priority = HIGH_PRIORITY;
			} else if (!isFinal)
				bugType = "MS_CANNOT_BE_FINAL";
			else
				throw new IllegalStateException("impossible");
			if (!isReadAnywhere) priority = LOW_PRIORITY;

			BugInstance bug = new BugInstance(this, bugType, priority)
												.addClass(className)
												.addField(f);
			SourceLineAnnotation firstPC = firstFieldUse.get(f);
			if (firstPC != null)
				bug.addSourceLine(firstPC);
			bugReporter.reportBug(bug);

		}
	}
}
