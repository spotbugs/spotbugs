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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.util.Util;

public class FieldSummary {
	private Set<XField> writtenOutsideOfConstructor = new HashSet<XField>();
	private Map<XField, OpcodeStack.Item> summary = new HashMap<XField, OpcodeStack.Item>();
	private Map<XMethod, Set<XField>> fieldsWritten = new HashMap<XMethod, Set<XField>>();
	
	private boolean complete = false;
	public OpcodeStack.Item getSummary(XField field) {
		if (field == null) return new OpcodeStack.Item();
		OpcodeStack.Item result = summary.get(field);
		if (result == null) {
	        String signature = field.getSignature();
	        return new OpcodeStack.Item(signature);
        }
		return result;
	}

	public void setFieldsWritten(XMethod method, Collection<XField> fields) {
		if (fields.isEmpty()) return;
		if (fields.size() == 1) {
			fieldsWritten.put(method, Collections.singleton(Util.first(fields)));
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
    	int fields = 0;
    	int removed = 0;
    	int retained = 0;
	    this.complete = complete;
	    if (isComplete()) {
	    	for(Iterator<Map.Entry<XField, OpcodeStack.Item>> i =  summary.entrySet().iterator(); i.hasNext(); ) {
	    		Map.Entry<XField, OpcodeStack.Item> entry = i.next();
	    		OpcodeStack.Item defaultItem = new OpcodeStack.Item(entry.getKey().getSignature());
	    		fields++;
	    		Item value = entry.getValue();
	    		value.makeCrossMethod();
				if (defaultItem.equals(value)) {
	    			i.remove();
	    			removed++;
	    		} else {
	    			retained++;
	    		}
	    	}
	    }
    }
	/**
     * @return Returns the complete.
     */
    public boolean isComplete() {
	    return complete;
    }


}
