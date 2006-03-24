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


import edu.umd.cs.findbugs.*;
import java.util.*;
import org.apache.bcel.classfile.*;

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

	static class FieldRecord {
		String className;
		String name;
		String signature;
		boolean isPublic;
		boolean isFinal;
	}


	LinkedList<FieldRecord> seen = new LinkedList<FieldRecord>();
	boolean publicClass;
	boolean zeroOnTOS;
	boolean emptyArrayOnTOS;
	boolean inStaticInitializer;
	String packageName;
	Set<String> unsafeValue = new HashSet<String>();
	Set<String> interfaces = new HashSet<String>();
	Set<String> notFinal = new HashSet<String>();
	Set<String> outsidePackage = new HashSet<String>();
	Map<String, SourceLineAnnotation> firstFieldUse = new HashMap<String, SourceLineAnnotation>();
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
		if ((flags & ACC_INTERFACE) != 0) {
			interfaces.add(getDottedClassName());
			}

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
			String name = (getClassConstantOperand() + "." + getNameConstantOperand())
			        .replace('/', '.');
			/*
			System.out.println("In " + betterClassName
				+ " accessing "
				+ (classConstant + "." + nameConstant)
				+ "	" + samePackage
				+ "	" + initOnly
				+ "	" + safeValue
				);
			*/

			if (!samePackage)
				outsidePackage.add(name);

			if (!initOnly)
				notFinal.add(name);

			if (!safeValue)
				unsafeValue.add(name);
			
			//Remove inStaticInitializer check to report all source lines of first use
			//doing so, however adds quite a bit of memory bloat.
			if (inStaticInitializer && !firstFieldUse.containsKey(name)) {
				SourceLineAnnotation sla = SourceLineAnnotation.fromVisitedInstruction(this);
				firstFieldUse.put(name, sla);
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

		FieldRecord f = new FieldRecord();
		f.className = getDottedClassName();
		f.name = getFieldName();
		f.signature = getDottedFieldSig();
		f.isPublic = isPublic;
		f.isFinal = isFinal;

		seen.add(f);

	}

	@Override
         public void report() {
		/*
		for(Iterator i = unsafeValue.iterator(); i.hasNext(); ) {
			System.out.println("Unsafe: " + i.next());
			}
		*/
		for (FieldRecord f : seen) {
			boolean isFinal = f.isFinal;
			String className = f.className;
			String fieldSig = f.signature;
			String fieldName = f.name;
			String name = className + "." + fieldName;
			boolean couldBeFinal = !isFinal
					&& !notFinal.contains(name);
			boolean isPublic = f.isPublic;
			boolean couldBePackage = !outsidePackage.contains(name);
			boolean movedOutofInterface = couldBePackage &&
					interfaces.contains(className);
			boolean isHashtable = fieldSig.equals("Ljava.util.Hashtable;");
			boolean isArray = fieldSig.charAt(0) == '['
					&& unsafeValue.contains(name);
			/*
			              System.out.println(className + "."  + fieldName
						              + " : " + fieldSig
					              + "	" + isHashtable
					              + "	" + isArray
						              );
			              */

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


			BugInstance bug = new BugInstance(this, bugType, priority)
												.addClass(className)
												.addField(className, fieldName, fieldSig, true);
			SourceLineAnnotation firstPC = firstFieldUse.get(className + "." + fieldName);
			if (firstPC != null)
				bug.addSourceLine(firstPC);
			bugReporter.reportBug(bug);

		}
	}
}
