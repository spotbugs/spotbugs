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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.annotation.WillClose;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.charsets.UTF8;

/**
 * Save bugs here, uses SortedBugCollection.writeXML()
 *
 * @author Dan
 *
 */
public class BugSaver {

    private static String lastPlaceSaved;

    public static void saveBugs(@WillClose Writer out, BugCollection data, Project p) {

        try {
            data.writeXML(out);
        } catch (IOException e) {
            Debug.println(e);
        }
    }

    public static void saveBugs(File out, BugCollection data, Project p) {
        try {
            saveBugs(UTF8.fileWriter(out), data, p);
            lastPlaceSaved = out.getAbsolutePath();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "An error has occurred in saving your file");
        }
    }

    public static String getLastPlaceSaved() {
        return lastPlaceSaved;
    }

}
