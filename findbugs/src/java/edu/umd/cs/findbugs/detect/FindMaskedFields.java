/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004, University of Maryland
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
import edu.umd.cs.findbugs.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.Repository;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindMaskedFields extends BytecodeScanningDetector implements Constants2 {
	private BugReporter bugReporter;
	private int numParms;
	private Map<String, FieldAnnotation> classFieldMap = new HashMap<String, FieldAnnotation>();
	private Map<String, FieldAnnotation> superclassFieldMap = new HashMap<String, FieldAnnotation>();
	private boolean staticMethod;

	public FindMaskedFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void visit(JavaClass obj) {
		if (obj.isInterface())
			return;
		classFieldMap.clear();
		superclassFieldMap.clear();

		// Build map of fields in this class
		Field[] fields = obj.getFields();
		String fieldName;
		for (int f = 0; f < fields.length; f++) {
			FieldAnnotation fa = FieldAnnotation.fromBCELField(obj.getClassName(), fields[f]);
			classFieldMap.put(fa.getFieldName(), fa);
		}

		// Build map of visible instance fields in superclasses
		// FIXME: would be nice to store these in a persistent data
		// structure, rather than recreating them for each
		// subclass.
		try {	
			JavaClass[] superClasses = org.apache.bcel.Repository.getSuperClasses(obj);
			for (int c = 0; c < superClasses.length; c++) {
				Field[] superFields = superClasses[c].getFields();
				for (int f = 0; f < superFields.length; f++) {
					Field superfield = superFields[f];

					// Only want instance fields that are guaranteed to be visible
					// FIXME: doesn't handle package-protected fields, which
					// might be obscured by another class in the same package
					if (superfield.isStatic() || !(superfield.isPublic() || superfield.isProtected()))
						continue;

					// Have we seen a superclass field with this name yet?
					if (superclassFieldMap.get(superfield.getName()) != null)
						continue;

					// Add it
					FieldAnnotation faSuper =
						FieldAnnotation.fromBCELField(superClasses[c].getClassName(), superfield);
					superclassFieldMap.put(superfield.getName(), faSuper);
				}
			}
		} catch (ClassNotFoundException e) { 
			bugReporter.reportMissingClass(e);
		}

		// Masked fields are the intersection of the fields defined
		// by the class and the visible superclass fields.
		Map<String, FieldAnnotation> intersection = new HashMap<String, FieldAnnotation>(superclassFieldMap);
		intersection.keySet().retainAll(classFieldMap.keySet());
		for (Iterator<FieldAnnotation> i = intersection.values().iterator(); i.hasNext(); ) {
			bugReporter.reportBug(new BugInstance("MF_CLASS_MASKS_FIELD", LOW_PRIORITY)
				.addClass(this)
				.addField(i.next()).describe("FIELD_SUPER"));
		}

		super.visit(obj);
	}
	
	public void visit(Method obj) {
		super.visit(obj);
		numParms = obj.getArgumentTypes().length;
		staticMethod = obj.isStatic();
	}
	
	public void visit(LocalVariableTable obj) {
		if (staticMethod)
			return;
			
		LocalVariable[] vars = obj.getLocalVariableTable();
		for (int v = numParms+1; v < vars.length; v++) { 
			LocalVariable var = vars[v];
			String varName = var.getName();
			if (varName.equals("serialVersionUID"))
				continue;
			FieldAnnotation fa;
			// TODO: we could distinguish between obscuring a field in the same class
			// vs. obscuring a field in a superclass.  Not sure how important that is.
			if ((fa = classFieldMap.get(varName)) != null || (fa = superclassFieldMap.get(varName)) != null) {
				if (var.getStartPC() > 0)
					bugReporter.reportBug(
						new BugInstance("MF_METHOD_MASKS_FIELD", NORMAL_PRIORITY)
							.addClassAndMethod(this)
							.addField(fa)
							.addSourceLine(this, var.getStartPC()-1));
			}
		}
		super.visit(obj);
	}
}

// vim:ts=4
