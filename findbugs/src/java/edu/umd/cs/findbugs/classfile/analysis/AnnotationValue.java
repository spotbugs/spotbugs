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

package edu.umd.cs.findbugs.classfile.analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;



/**
 * @author pwilliam
 */
public class AnnotationValue implements AnnotationVisitor {
	/**
	 * @author pwilliam
	 */
	private final class AnnotationArrayVisitor implements AnnotationVisitor {
		/**
		 * 
		 */
		private final String name;

		private final List<Object> outerList;

		/**
		 * 
		 */
		private final List<Object> result = new LinkedList<Object>();

		/**
		 * @param name
		 * @param result
		 */
		private AnnotationArrayVisitor(String name) {
			this.name = name;
			this.outerList = null;
		}

		private AnnotationArrayVisitor(List<Object> outerList) {
			this.name = null;
			this.outerList = outerList;
		}

		public void visit(String name, Object value) {
			result.add(value);
		}

		public AnnotationVisitor visitAnnotation(String name, String desc) {
			AnnotationValue newValue = new AnnotationValue();
			result.add(newValue);
			return newValue;
		}

		public AnnotationVisitor visitArray(String name) {
			return new AnnotationArrayVisitor(result);
		}

		public void visitEnd() {
			if (name != null)
				valueMap.put(name, result.toArray());
			else
				outerList.add(result.toArray());
		}

		public void visitEnum(String name, String desc, String value) {
			result.add(new EnumValue(desc, value));

		}
	}

	Map<String, Object> valueMap = new HashMap<String, Object>();

	Map<String, Object> typeMap = new HashMap<String, Object>();

	public String toString() {
		return valueMap.toString();
	}
	public void visit(String name, Object value) {
		valueMap.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.objectweb.asm.AnnotationVisitor#visitAnnotation(java.lang.String,
	 *      java.lang.String)
	 */
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		AnnotationValue newValue = new AnnotationValue();
		valueMap.put(name, newValue);
		typeMap.put(name, desc);
		return newValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.objectweb.asm.AnnotationVisitor#visitArray(java.lang.String)
	 */
	public AnnotationVisitor visitArray(final String name) {
		return new AnnotationArrayVisitor(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
	 */
	public void visitEnd() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.objectweb.asm.AnnotationVisitor#visitEnum(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void visitEnum(String name, String desc, String value) {
		valueMap.put(name, new EnumValue(desc, value));
		typeMap.put(name, desc);

	}

}
