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

package edu.umd.cs.findbugs.ba.type;

/**
 * Exception to indicate that information requested about
 * a type is not known.  For example, asking whether
 * a particular type is an interface or a class, when
 * we don't know because we haven't seen the class file
 * for that type.
 *
 * @author David Hovemeyer
 */
public class UnknownTypeException extends ClassNotFoundException {
	public UnknownTypeException(String msg) {
		super(msg);
	}
}

// vim:ts=4
