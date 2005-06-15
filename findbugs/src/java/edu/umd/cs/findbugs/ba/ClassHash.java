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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

/**
 * Compute a hash of method names and signatures.
 * This allows us to find out when a class has been renamed, but
 * not changed in any other obvious way.
 * 
 * @author David Hovemeyer
 */
public class ClassHash {
	private JavaClass javaClass;
	private MessageDigest digest;
	private byte[] hash;
	
	public ClassHash(JavaClass javaClass) throws NoSuchAlgorithmException {
		this.javaClass = javaClass;
		this.digest = MessageDigest.getInstance("MD5");
	}
	
	public ClassHash computeHash() {
		Method[] methodList = new Method[javaClass.getMethods().length];
		
		// Sort methods
		System.arraycopy(javaClass.getMethods(), 0, methodList, 0, javaClass.getMethods().length);
		Arrays.sort(methodList, new Comparator<Method>() {
			public int compare(Method o1, Method o2) {
				// sort by name, then signature
				int cmp = o1.getName().compareTo(o2.getName());
				if (cmp != 0)
					return cmp;
				return o1.getSignature().compareTo(o2.getSignature());
				
			}
		});
		
		// Compute digest of names and signatures, in order
		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			work(method.getName(), encoder); 
			work(method.getSignature(), encoder); 
		}
		
		hash = digest.digest();
		
		return this;
	}
	
	private void work(String s, CharsetEncoder encoder) {
		try {
			CharBuffer cbuf = CharBuffer.allocate(s.length());
			cbuf.append(s);
			ByteBuffer buf = encoder.encode(cbuf);
			int nbytes = buf.remaining();
			byte[] encodedBytes = new byte[nbytes];
			buf.get(encodedBytes);
			digest.update(encodedBytes);
		} catch (CharacterCodingException e) {
			// This should never happen, since we're encoding to UTF-8.
		}
	}

	public byte[] getHash() {
		return hash;
	}
}
