/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.ba.npe;

import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.ba.vna.ValueNumber;

public   class WillBeDereferencedInfo   {
		
		Set<ValueNumber> value = new HashSet<ValueNumber>();
		boolean isTop;

		public static WillBeDereferencedInfo getTop() {
			WillBeDereferencedInfo top = new WillBeDereferencedInfo();
			top.isTop = true;
			return top;
		}
		
		public  WillBeDereferencedInfo copy() {
			WillBeDereferencedInfo w = new WillBeDereferencedInfo();
			w.copyFrom(this);
			return w;
		}
		
		public  void copyFrom	(WillBeDereferencedInfo w) {
			this.isTop = w.isTop;
			this.value.clear();
			this.value.addAll(w.value);
		}
		public  void meet(WillBeDereferencedInfo source) {
			if (source.isTop) return;
			if (isTop) {
				isTop = false;
				value.clear();
				value.addAll(source.value);
				return;
			}
			value.retainAll(source.value);
		}
		
		@Override
                 public int hashCode() {
			if (isTop) return 42;
			return value.hashCode();
		}
		@Override
                 public boolean equals(Object o) {
			if (o == null || o.getClass() != WillBeDereferencedInfo.class) return false;
			WillBeDereferencedInfo w = (WillBeDereferencedInfo) o;
			if (isTop != w.isTop) return false;
			return value.equals(w.value);
		}
		
		

	}
