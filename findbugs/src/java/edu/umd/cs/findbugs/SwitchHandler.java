/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius
 * Copyright (C) 2005 University of Maryland
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
package edu.umd.cs.findbugs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.visitclass.DismantleBytecode;


public class SwitchHandler
{
	private List<SwitchDetails> switchOffsetStack;
	
	public SwitchHandler() {
		switchOffsetStack = new ArrayList<SwitchDetails>();
	}
	
	public void enterSwitch( DismantleBytecode dbc ) {
		switchOffsetStack.add(
				new SwitchDetails( dbc.getPC(), 
								   dbc.getSwitchOffsets(),
								   dbc.getDefaultSwitchOffset()));
	}
	
	public boolean inSwitch( DismantleBytecode dbc ) {
		return getNextSwitchOffset(dbc) >= 0;
	}
	
	public int getNextSwitchOffset( DismantleBytecode dbc ) {
		int size = switchOffsetStack.size();
		while (size > 0) {
			SwitchDetails details = switchOffsetStack.get(size-1);
			
			int nextSwitchOffset = details.getNextSwitchOffset(dbc.getPC());
			if (nextSwitchOffset >= 0)
				return nextSwitchOffset;

			switchOffsetStack.remove(size-1);
			size--;
		}
		
		return -1;
	}
	
	public class SwitchDetails
	{
		int   switchPC;
		int[] swOffsets;
		int	  defaultOffset;
		int   nextOffset;
		
		public SwitchDetails(int pc, int[] offsets, int defOffset) {
			switchPC = pc;
			swOffsets = new int[offsets.length+1];
			System.arraycopy( offsets, 0, swOffsets, 0, offsets.length );
			swOffsets[offsets.length] = defOffset;
			Arrays.sort(swOffsets);
			defaultOffset = defOffset;
			nextOffset = 0;
		}	
		
		public int getNextSwitchOffset(int currentPC) {
			while ((nextOffset < swOffsets.length) && (currentPC > (switchPC + swOffsets[nextOffset])))
				nextOffset++;
			
			if (nextOffset >= swOffsets.length)
				return -1;
			
			return switchPC + swOffsets[nextOffset];
		}
		
		public boolean inSwitch(int currentPC) {
			return getNextSwitchOffset(currentPC) >= 0;
		}
	}
}
