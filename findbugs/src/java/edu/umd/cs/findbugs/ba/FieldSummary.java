/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;

public class FieldSummary {
	private Set<XField> writtenOutsideOfConstructor = new HashSet<XField>();
	private Map<XField, OpcodeStack.Item> summary = new HashMap<XField, OpcodeStack.Item>();
	private Map<XMethod, Set<XField>> fieldsWritten = new HashMap<XMethod, Set<XField>>();
	
	private boolean complete = false;
	public OpcodeStack.Item getSummary(XField field) {
		OpcodeStack.Item result = summary.get(field);
		if (result == null)
			return new OpcodeStack.Item();
		return result;
	}

	public void setFieldsWritten(XMethod method, Collection<XField> fields) {
		if (fields.isEmpty()) return;
		if (fields.size() == 1) {
			fieldsWritten.put(method, Collections.singleton(fields.iterator().next()));
			return;
		}
		fieldsWritten.put(method, new HashSet<XField>(fields));
	}
	
	public Set<XField> getFieldsWritten(XMethod method) {
		Set<XField> result = fieldsWritten.get(method);
		if (result == null) return Collections.<XField>emptySet();
		return result;
	}
	public boolean isWrittenOutsideOfConstructor(XField field) {
		if (field.isFinal())
			return false;
		boolean result = writtenOutsideOfConstructor.contains(field);
		return result;
	}

	
	public boolean addWrittenOutsideOfConstructor(XField field) {
		return writtenOutsideOfConstructor.add(field);
	}

	public void mergeSummary(XField fieldOperand, OpcodeStack.Item mergeValue) {
		OpcodeStack.Item oldSummary = summary.get(fieldOperand);
		if (oldSummary != null) {
			Item newValue = OpcodeStack.Item.merge(mergeValue, oldSummary);
			summary.put(fieldOperand, newValue);
		} else
			summary.put(fieldOperand, mergeValue);
    }

	/**
     * @param complete The complete to set.
     */
    public void setComplete(boolean complete) {
	    this.complete = complete;
    }

	/**
     * @return Returns the complete.
     */
    public boolean isComplete() {
	    return complete;
    }


}
