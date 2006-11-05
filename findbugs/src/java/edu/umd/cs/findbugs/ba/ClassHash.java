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

import org.apache.bcel.classfile.Field;
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
public class ClassHash implements XMLWriteable, Comparable<ClassHash> {
	/**
	 * XML element name for a ClassHash.
	 */
	public static final String CLASS_HASH_ELEMENT_NAME = "ClassHash";
	
	/**
	 * XML element name for a MethodHash.
	 */
	public static final String METHOD_HASH_ELEMENT_NAME = "MethodHash";
	
	// Fields
	private String className;
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
	public ClassHash(String className, byte[] classHash) {
		this();
		this.className = className;
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
		methodHashMap.put(method, new MethodHash(
				method.getName(),
				method.getSignature(),
				method.isStatic(),
				methodHash));
	}
	
	/**
	 * @return Returns the className.
	 */
	public String getClassName() {
		return className;
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
	 * Set class hash.
	 * 
	 * @param classHash the class hash value to set
	 */
	public void setClassHash(byte[] classHash) {
		this.classHash= new byte[classHash.length];
		System.arraycopy(classHash, 0, this.classHash, 0, classHash.length);
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
	 */
	public ClassHash computeHash(JavaClass javaClass) {
		this.className = javaClass.getClassName();
		
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
		
		Field[] fieldList = new Field[javaClass.getFields().length];
		
		// Sort fields
		System.arraycopy(javaClass.getFields(), 0, fieldList, 0, javaClass.getFields().length);
		Arrays.sort(fieldList, new Comparator<Field>() {
			/* (non-Javadoc)
			 * @see java.util.Comparator#compare(T, T)
			 */
			public int compare(Field o1, Field o2) {
				int cmp = o1.getName().compareTo(o2.getName());
				if (cmp != 0)
					return cmp;
				return o1.getSignature().compareTo(o2.getSignature());
			}
		});
		
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("No algorithm for computing class hash", e);
		}
		
		// Compute digest of method names and signatures, in order.
		// Also, compute method hashes.
		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		for (Method method : methodList) {
			work(digest, method.getName(), encoder); 
			work(digest, method.getSignature(), encoder);
			
			MethodHash methodHash = new MethodHash().computeHash(method);
			methodHashMap.put(XFactory.createXMethod(javaClass, method), methodHash);
		}
		
		// Compute digest of field names and signatures.
		for (Field field : fieldList) {
			work(digest, field.getName(), encoder);
			work(digest, field.getSignature(), encoder);
		}
		
		classHash = digest.digest();
		
		return this;
	}
	
	private static void work(MessageDigest digest, String s, CharsetEncoder encoder) {
		try {
			CharBuffer cbuf = CharBuffer.allocate(s.length());
			cbuf.put(s);
			cbuf.flip();
			
			ByteBuffer buf = encoder.encode(cbuf);
//			System.out.println("pos="+buf.position() +",limit=" + buf.limit());
			int nbytes = buf.limit();
			byte[] encodedBytes = new byte[nbytes];
			buf.get(encodedBytes);
			
			digest.update(encodedBytes);
		} catch (CharacterCodingException e) {
			// This should never happen, since we're encoding to UTF-8.
		}
	}
	
	public void writeXML(XMLOutput xmlOutput) throws IOException {
		xmlOutput.startTag(CLASS_HASH_ELEMENT_NAME);
		xmlOutput.addAttribute("class", className);
		xmlOutput.addAttribute("value", hashToString(classHash));
		xmlOutput.stopTag(false);

		for (Map.Entry<XMethod, MethodHash> entry : methodHashMap.entrySet()) {
			xmlOutput.startTag(METHOD_HASH_ELEMENT_NAME);
			xmlOutput.addAttribute("name", entry.getKey().getName());
			xmlOutput.addAttribute("signature", entry.getKey().getSignature());
			xmlOutput.addAttribute("isStatic", String.valueOf(entry.getKey().isStatic()));
			xmlOutput.addAttribute("value", hashToString(entry.getValue().getMethodHash()));
			xmlOutput.stopTag(true);
		}
		
		xmlOutput.closeTag(CLASS_HASH_ELEMENT_NAME);
	}
	
	private static final char[] HEX_CHARS = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
	};

	/**
	 * Convert a hash to a string of hex digits.
	 *
	 * @param hash the hash
	 * @return a String representation of the hash
	 */
	public static String hashToString(byte[] hash) {
		StringBuffer buf = new StringBuffer();
		for (byte b : hash) {
			buf.append(HEX_CHARS[(b >> 4) & 0xF]);
			buf.append(HEX_CHARS[b & 0xF]);
		}
		return buf.toString();
	}

	private static int hexDigitValue(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		else if (c >= 'a' && c <= 'f')
			return 10 + (c - 'a');
		else if (c >= 'A' && c <= 'F')
			return 10 + (c - 'A');
		else
			throw new IllegalArgumentException("Illegal hex character: " + c);
	}
	
	/**
	 * Convert a string of hex digits to a hash.
	 * 
	 * @param s string of hex digits
	 * @return the hash value represented by the string
	 */
	public static byte[] stringToHash(String s) {
		if (s.length() % 2 != 0)
			throw new IllegalArgumentException("Invalid hash string: " + s);
		byte[] hash = new byte[s.length() / 2];
		for (int i = 0; i < s.length(); i += 2) {
			byte b = (byte) ((hexDigitValue(s.charAt(i)) << 4) + hexDigitValue(s.charAt(i+1)));
			hash[i / 2] = b;
		}
		return hash;
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

	public int hashCode() {
		return Arrays.hashCode(classHash);
	}
	public boolean equals(Object o) {
		if (!(o instanceof ClassHash)) return false;
		return isSameHash((ClassHash)o);
	}
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(T)
	 */
	public int compareTo(ClassHash other) {
		int cmp = MethodHash.compareHashes(this.classHash, other.classHash);
		//System.out.println(this + " <=> " + other +  ": compareTo=" + cmp);
		return cmp;
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	//@Override
	@Override
         public String toString() {
		return getClassName() + ":" + hashToString(this.classHash);
	}
}
