/*
 * ConsoleLogger.java
 *
 * Created on April 3, 2003, 1:37 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.Date;

/**
 *
 * @author David Hovemeyer
 */
public class ConsoleLogger {

    public static final int INFO = 0;
    public static final int ERROR = 1;
    
    private FindBugsFrame frame;
    
    /** Creates a new instance of ConsoleLogger */
    public ConsoleLogger(FindBugsFrame frame) {
        this.frame = frame;
    }

    public void logMessage(int severity, String message) {
        Date date = new Date();
        StringBuffer buf = new StringBuffer();
        buf.append('[');
        buf.append(date.toString());
        buf.append("] ");
        if (severity == ERROR)
            buf.append("ERROR: ");
        buf.append(message);
        frame.writeToConsole(buf.toString());
    }
    
}
