/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

import java.util.*;

/**
 * A simple class to parse method signatures.
 *
 * @author David Hovemeyer
 */
public class SignatureParser {
	private class ParameterSignatureIterator implements Iterator<String> {
		private int index = 1;

		public boolean hasNext() {
			return index < signature.length()
			        && signature.charAt(index) != ')';
		}

		public String next() {
			if (!hasNext()) throw new NoSuchElementException();
			int ch = signature.charAt(index);
			String result;
			switch (ch) {
			case 'B':
			case 'C':
			case 'D':
			case 'F':
			case 'I':
			case 'J':
			case 'S':
			case 'Z':
				result = signature.substring(index, index + 1);
				++index;
				break;

			case 'L':
				int semi = signature.indexOf(';', index + 1);
				if (semi < 0)
					throw new IllegalStateException("Invalid method signature: " + signature);
				result = signature.substring(index, semi + 1);
				index = semi + 1;
				break;

			case 'V':
			default:
				throw new IllegalStateException("Invalid method signature: " + signature);
			}

			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private String signature;

	/**
	 * Constructor.
	 *
	 * @param signature the method signature to be parsed
	 */
	public SignatureParser(String signature) {
		if (!signature.startsWith("("))
			throw new IllegalStateException("Bad method signature: " + signature);
		this.signature = signature;
	}

	/**
	 * Get an Iterator over signatures of the method parameters.
	 *
	 * @return Iterator which returns the parameter type signatures in order
	 */
	public Iterator<String> parameterSignatureIterator() {
		return new ParameterSignatureIterator();
	}
}

// vim:ts=4
