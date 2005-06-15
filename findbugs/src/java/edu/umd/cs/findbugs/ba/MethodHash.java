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

package edu.umd.cs.findbugs.ba;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.bcel.classfile.Method;


/**
 * Compute a hash of the bytecode for given method. 
 * This can find methods which have not been changed other
 * than accessing different constant pool entries.
 * 
 * @author David Hovemeyer
 */
public class MethodHash {
	private Method method;
	private MessageDigest digest;
	private byte[] hash;

	/**
	 * Constructor.
	 * 
	 * @param method method to compute bytecode hash for
	 * @throws NoSuchAlgorithmException
	 */
	public MethodHash(Method method) throws NoSuchAlgorithmException {
		this.method = method;
		this.digest = MessageDigest.getInstance("MD5");
	}
	
	public MethodHash computeHash() {
		byte[] code;
		if (method.getCode() == null || method.getCode().getCode() == null) {
			code = new byte[0];
		} else {
			code = method.getCode().getCode();
		}
		
		BytecodeScanner.Callback callback = new BytecodeScanner.Callback() {
			public void handleInstruction(int opcode, int index) {
				digest.update((byte) opcode);
			}
		};
		
		BytecodeScanner bytecodeScanner = new BytecodeScanner();
		bytecodeScanner.scan(code, callback);
		
		hash = digest.digest();
		
		return this;
	}
	
	public byte[] getHash() {
		return hash;
	}
}
