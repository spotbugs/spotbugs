/*
 * Bytecode analysis framework
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba.type;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy;

/**
 * Field property storing the types of values stored
 * in a field.  The idea is that we may be able to determine
 * a more precise type for values loaded from the field
 * than the field type alone would indicate.
 * 
 * @author David Hovemeyer
 */
public class FieldStoreType {
	private HashSet<String> typeSignatureSet;
	private ReferenceType loadType;

	public FieldStoreType() {
		this.typeSignatureSet = new HashSet<String>();
	}

	// TODO: type may be exact
	public void addTypeSignature(String signature) {
		loadType = null;
		typeSignatureSet.add(signature);
	}

	public Iterator<String> signatureIterator() {
		return typeSignatureSet.iterator();
	}

	public ReferenceType getLoadType(ReferenceType fieldType) {
		if (loadType == null) {
			computeLoadType(fieldType);
		}
		return loadType;
	}

	private void computeLoadType(ReferenceType fieldType) {
		ReferenceType leastSupertype = null;

		for (Iterator<String> i = signatureIterator(); i.hasNext();) {
			try {
				String signature = i.next();
				Type type = Type.getType(signature);
				if (!(type instanceof ReferenceType))
					continue;

				// FIXME: this will mangle interface types, since
				// getFirstCommonSuperclass() ignores interfaces.
				leastSupertype = (leastSupertype == null)
					? (ReferenceType) type
					: leastSupertype.getFirstCommonSuperclass((ReferenceType) type);

			} catch (ClassFormatException e) {
				// Bad signature: ignore
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
		}

		try {
			if (leastSupertype != null && Hierarchy.isSubtype(leastSupertype, fieldType))
				loadType = leastSupertype;
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}

		if (loadType == null)
			loadType = fieldType;
	}
}
