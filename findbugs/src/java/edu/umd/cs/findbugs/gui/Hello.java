/*
 * Hello.java
 *
 * Created on March 29, 2003, 1:52 AM
 */

package edu.umd.cs.findbugs.gui;

/**
 * This is just a test of creating a class in NetBeans.
 * @author  David Hovemeyer
 */
public class Hello {
    
    private String toWhom;
    
    /** Creates a new instance of Hello. */
    public Hello() {
	this.toWhom = "you";
    }
    
    /**
     * Creates a new instance of Hello.
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
