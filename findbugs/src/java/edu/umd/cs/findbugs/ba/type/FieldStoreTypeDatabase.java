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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.interproc.FieldPropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;

/**
 * @author David Hovemeyer
 */
public class FieldStoreTypeDatabase
	extends FieldPropertyDatabase<FieldStoreType> {

	public static final String DEFAULT_FILENAME = "fieldStoreTypes.db";

	public void purgeBoringEntries() {
		Collection<XField> keys = new ArrayList<XField>(getKeys());
		for(XField f : keys) {
            String s = f.getSignature();
			FieldStoreType type = getProperty(f);
			Type fieldType = Type.getType(f.getSignature());
			if (!(fieldType instanceof ReferenceType)) {
                removeProperty(f);
				continue;
			}
			ReferenceType storeType = type.getLoadType((ReferenceType)fieldType);
            if (storeType.equals(fieldType)) 
				removeProperty(f);
		}
	}
	
	@Override
		 protected FieldStoreType decodeProperty(String propStr) throws PropertyDatabaseFormatException {
		FieldStoreType property = new FieldStoreType();
		StringTokenizer t = new StringTokenizer(propStr, ",");
		while (t.hasMoreTokens()) {
			String signature = t.nextToken();
			property.addTypeSignature(signature);
		}
		return property;
	}


	@Override
		 protected String encodeProperty(FieldStoreType property) {
		StringBuffer buf = new StringBuffer();
		for (Iterator<String> i = property.signatureIterator(); i.hasNext();) {
			if (buf.length() > 0) {
				buf.append(',');
			}
			buf.append(i.next());
		}
		return buf.toString();
	}

}
