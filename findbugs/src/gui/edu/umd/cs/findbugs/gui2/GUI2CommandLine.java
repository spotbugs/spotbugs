/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

package edu.umd.cs.findbugs.gui2;

import java.io.File;
import java.io.IOException;

import javax.swing.UIManager;

import edu.umd.cs.findbugs.FindBugsCommandLine;

/**
 * Command line switches/options for GUI2.
 *
 * @author David Hovemeyer
 */
public class GUI2CommandLine extends FindBugsCommandLine {
    private float fontSize = 12;

    private boolean fontSizeSpecified = false;

    private boolean docking = true;

    private int priority = Thread.NORM_PRIORITY - 1;

    private File saveFile;

    public GUI2CommandLine() {
        // Additional constuctor just as hack for decoupling the core package
        // from gui2 package
        // please add all options in the super class
        super(true);
    }

    @Override
    protected void handleOption(String option, String optionExtraPart) {
        if ("-clear".equals(option)) {
            GUISaveState.clear();
            System.exit(0);
        } else if ("-d".equals(option) || "--nodock".equals(option)) {
            docking = false;
        } else if ("-look".equals(option)) {
            String arg = optionExtraPart;
            String theme = null;

            if ("plastic".equals(arg)) {
                // You can get the Plastic look and feel from jgoodies.com:
                // http://www.jgoodies.com/downloads/libraries.html
                // Just put "plastic.jar" in the lib directory, right next
                // to the other jar files.
                theme = "com.jgoodies.plaf.plastic.PlasticXPLookAndFeel";
            } else if ("gtk".equals(arg)) {
                theme = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
            } else if ("native".equals(arg)) {
                theme = UIManager.getSystemLookAndFeelClassName();
            } else {
                System.err.println("Style '" + arg + "' not supported");
            }

            if (theme != null) {
                try {
                    UIManager.setLookAndFeel(theme);
                } catch (Exception e) {
                    System.err.println("Couldn't load " + arg + " look and feel: " + e.toString());
                }
            }
        } else {
            super.handleOption(option, optionExtraPart);
        }
    }

    @Override
    protected void handleOptionWithArgument(String option, String argument) throws IOException {
        if ("-f".equals(option)) {
            try {
                fontSize = Float.parseFloat(argument);
                fontSizeSpecified = true;
            } catch (NumberFormatException e) {
                // ignore
            }
        } else if ("-priority".equals(option)) {
            try {
                priority = Integer.parseInt(argument);
            } catch (NumberFormatException e) {
                // ignore
            }
        } else if ("-loadBugs".equals(option) || "-loadbugs".equals(option)) {
            saveFile = new File(argument);
            if (!saveFile.exists()) {
                System.err.println("Bugs file \"" + argument + "\" could not be found");
                System.exit(1);
            }
        } else {
            super.handleOptionWithArgument(option, argument);
        }
    }

    public float getFontSize() {
        return fontSize;
    }

    public boolean isFontSizeSpecified() {
        return fontSizeSpecified;
    }

    public boolean getDocking() {
        return docking;
    }

    public void setDocking(boolean docking) {
        this.docking = docking;
    }

    public int getPriority() {
        return priority;
    }

    public File getSaveFile() {
        return saveFile;
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }
}
