/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

/*
 * Hello.java
 *
 * Created on March 29, 2003, 1:52 AM
 */

package edu.umd.cs.findbugs.gui;

/**
 * This is just a test of creating a class in NetBeans.
 *
 * @author David Hovemeyer
 */
public class Hello {

	private String toWhom;

	/**
	 * Creates a new instance of Hello.
	 */
	public Hello() {
		this.toWhom = "you";
	}

	/**
	 * Creates a new instance of Hello.
	 *
	 * @param toWhom person to greet
	 */
	public Hello(String toWhom) {
		this.toWhom = toWhom;
	}

	/**
	 * Display a friendly greeting.
	 */
	public void greet() {
		System.out.println("Hello, " + toWhom);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Hello h;
		if (args.length > 0)
			h = new Hello(args[0]);
		else
			h = new Hello();
		h.greet();
	}

}
