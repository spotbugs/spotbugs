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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * Compute a hash of method names and signatures.
 * This allows us to find out when a class has been renamed, but
 * not changed in any other obvious way.
 * 
 * @author David Hovemeyer
 */
public class ClassHash implements XMLWriteable {
	private byte[] classHash;
	private Map<XMethod, MethodHash> methodHashMap;
	
	/**
	 * Constructor.
	 */
	public ClassHash() {
		this.methodHashMap = new HashMap<XMethod, MethodHash>();
	}

	/**
	 * Constructor.
	 * 
	 * @param classHash pre-computed class hash
	 */
	public ClassHash(byte[] classHash) {
		this();
		this.classHash = new byte[classHash.length];
		System.arraycopy(classHash, 0, this.classHash, 0, classHash.length);
	}

	/**
	 * Set method hash for given method.
	 * 
	 * @param method     the method
	 * @param methodHash the method hash
	 */
	public void setMethodHash(XMethod method, byte[] methodHash) {
		methodHashMap.put(method, new MethodHash(methodHash));
	}

	/**
	 * Get class hash.
	 * 
	 * @return the class hash
	 */
	public byte[] getClassHash() {
		return classHash;
	}
	
	/**
	 * Get method hash for given method.
	 * 
	 * @param method the method
	 * @return the MethodHash
	 */
	public MethodHash getMethodHash(XMethod method) {
		return methodHashMap.get(method);
	}
	
	/**
	 * Compute hash for given class and all of its methods.
	 * 
	 * @param javaClass the class
	 * @return this object
	 * @throws NoSuchAlgorithmException
	 */
	public ClassHash computeHash(JavaClass javaClass) throws NoSuchAlgorithmException {
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
		
		MessageDigest digest = MessageDigest.getInstance("MD5");
		
		// Compute digest of names and signatures, in order.
		// Also, compute method hashes.
		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			work(digest, method.getName(), encoder); 
			work(digest, method.getSignature(), encoder);
			
			try {
				MethodHash methodHash = new MethodHash().computeHash(method);
				methodHashMap.put(XMethodFactory.createXMethod(javaClass, method), methodHash);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("No algorithm for computing method hash", e);
			}
			
		}
		
		classHash = digest.digest();
		
		return this;
	}
	
	private static void work(MessageDigest digest, String s, CharsetEncoder encoder) {
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
	
	public void writeXML(XMLOutput xmlOutput) throws IOException {
		xmlOutput.startTag("ClassHash");
		xmlOutput.addAttribute("value", hashToString(classHash));
		xmlOutput.stopTag(false);

		for (Map.Entry<XMethod, MethodHash> entry : methodHashMap.entrySet()) {
			xmlOutput.startTag("MethodHash");
			xmlOutput.addAttribute("name", entry.getKey().getName());
			xmlOutput.addAttribute("signature", entry.getKey().getSignature());
			xmlOutput.addAttribute("isStatic", String.valueOf(entry.getKey().isStatic()));
			xmlOutput.addAttribute("value", hashToString(entry.getValue().getMethodHash()));
			xmlOutput.stopTag(true);
		}
		
		xmlOutput.closeTag("ClassHash");
	}
	
	private static final char[] HEX_CHARS = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
	};

	private static String hashToString(byte[] hash) {
		StringBuffer buf = new StringBuffer();
		for (byte b : hash) {
			buf.append(HEX_CHARS[(b >> 4) & 0xF]);
			buf.append(HEX_CHARS[b & 0xF]);
		}
		return buf.toString();
	}

	/**
	 * Return whether or not this class hash has the same hash value
	 * as the one given.
	 * 
	 * @param other another ClassHash
	 * @return true if the hash values are the same, false if not
	 */
	public boolean isSameHash(ClassHash other) {
		return Arrays.equals(classHash, other.classHash);
	}
}
