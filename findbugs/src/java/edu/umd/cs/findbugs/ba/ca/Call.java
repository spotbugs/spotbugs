/*
 * Bytecode Analysis Framework
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
package edu.umd.cs.findbugs.ba.ca;

public class Call {
	private final String className;
	private final String methodName;
	private final String methodSig;
	
	public Call(String className, String methodName, String methodSig) {
		this.className = className;
		this.methodName= methodName;
		this.methodSig = methodSig;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getMethodSig() {
		return methodSig;
	}
	
	//@Override
	@Override
         public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		Call other = (Call) obj;
		return this.className.equals(other.className)
			&& this.methodName.equals(other.methodName)
			&& this.methodSig.equals(other.methodSig);
	}
	
	//@Override
	@Override
         public int hashCode() {
		return className.hashCode() + methodName.hashCode() + methodSig.hashCode();
	}
}
