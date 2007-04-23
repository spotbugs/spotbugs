/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba.vna;

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.AbstractDataflow;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.SignatureParser;

public class ValueNumberDataflow extends AbstractDataflow<ValueNumberFrame, ValueNumberAnalysis> {
	public ValueNumberDataflow(CFG cfg, ValueNumberAnalysis analysis) {
		super(cfg, analysis);
	}

	/**
	 * Build map of value numbers to param indices.
	 * The first parameter has index 0, the second has index 1, etc.
	 * 
	 * @param method the method analyzed by the ValueNumberAnalysis
	 * @return the value number to parameter index map
	 */
	public Map<ValueNumber, Integer> getValueNumberToParamMap(Method method) {
		return getValueNumberToParamMap(method.getSignature(), method.isStatic());
	}

	/**
	 * Build map of value numbers to param indices.
	 * The first parameter has index 0, the second has index 1, etc.
	 * 
	 * @param methodSignature signature of the method analyzed by the ValueNumberAnalysis
	 * @param isStatic        true if the method is static, false if not
	 * @return the value number to parameter index map
	 */
	public Map<ValueNumber, Integer> getValueNumberToParamMap(String methodSignature, boolean isStatic) {
		HashMap<ValueNumber, Integer> valueNumberToParamMap =
			new HashMap<ValueNumber, Integer>();

		ValueNumberFrame frameAtEntry = getStartFact(getCFG().getEntry());

		int numParams = new SignatureParser(methodSignature).getNumParameters(); 
		int shift = isStatic ? 0 : 1;
		for (int i = 0; i < numParams; ++i) {
			valueNumberToParamMap.put(
					frameAtEntry.getValue(i + shift), (Integer)(i));
		}

		return valueNumberToParamMap;

	}
}

// vim:ts=4
