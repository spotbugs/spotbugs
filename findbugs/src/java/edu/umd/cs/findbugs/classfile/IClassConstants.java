/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile;

/**
 * @author David Hovemeyer
 */
public interface IClassConstants {
	public static final int MAGIC = 0xCAFEBABE;

	public static final int CONSTANT_Class 	 = 7;
	public static final int CONSTANT_Fieldref 	= 9;
	public static final int CONSTANT_Methodref 	= 10;
	public static final int CONSTANT_InterfaceMethodref 	= 11;
	public static final int CONSTANT_String 	= 8;
	public static final int CONSTANT_Integer 	= 3;
	public static final int CONSTANT_Float 	= 4;
	public static final int CONSTANT_Long 	= 5;
	public static final int CONSTANT_Double 	= 6;
	public static final int CONSTANT_NameAndType = 	12;
	public static final int CONSTANT_Utf8 	= 1;

	public static final int ACC_PUBLIC			= 0x0001;
	public static final int ACC_PRIVATE			= 0x0002;
	public static final int ACC_PROTECTED		= 0x0004;
	public static final int ACC_STATIC			= 0x0008;
	public static final int ACC_FINAL			= 0x0010;
	public static final int ACC_SUPER			= 0x0020;
	public static final int ACC_VOLATILE		= 0x0040;
	public static final int ACC_TRANSIENT		= 0x0080;	
	public static final int ACC_INTERFACE		= 0x0200;
	public static final int ACC_ABSTRACT 		= 0x0400;
	public static final int ACC_SYNTHETIC 		= 0x1000;
	public static final int ACC_ANNOTATION 		= 0x2000;
	public static final int ACC_ENUM     		= 0x4000;
	
	public static final int ACC_SYNCHRONIZED 	= 0x0020;
	public static final int ACC_BRIDGE 			= 0x0040;
	public static final int ACC_VARARGS 		= 0x0080;
	public static final int ACC_NATIVE 			= 0x0100;
	public static final int ACC_STRICT			= 0x0800;
	
}
