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

package edu.umd.cs.findbugs;

import java.io.PrintStream;

/**
 * Display FindBugs progress in the terminal window using ASCII codes. We assume
 * that the terminal window is at least 80 characters wide.
 *
 * @author David Hovemeyer
 */
public class TextUIProgressCallback implements FindBugsProgress {
    private final PrintStream out;

    private int goal;

    private int count;

    private int numPasses;

    private int pass;

    public TextUIProgressCallback(PrintStream out) {
        this.out = out;
    }

    @Override
    public void reportNumberOfArchives(int numArchives) {
        this.goal = numArchives;
        this.count = 0;
        scanningArchives(0);
    }

    @Override
    public void finishArchive() {
        scanningArchives(++count);
    }

    @Override
    public void predictPassCount(int[] classesPerPass) {
        out.println();
        printMessage(classesPerPass.length + " analysis passes to perform");
        this.numPasses = classesPerPass.length;
        this.pass = 0;
    }

    @Override
    public void startAnalysis(int numClasses) {
        if (pass == 0) {
            out.println();
        }
        this.goal = numClasses;
        this.count = 0;
        analyzingClasses(0);
    }

    @Override
    public void finishClass() {
        analyzingClasses(++count);
    }

    @Override
    public void finishPerClassAnalysis() {
        out.println();
        ++pass;
        if (pass == numPasses) {
            out.println("Done with analysis");
        }
    }

    private void scanningArchives(int i) {
        String msg = String.format("Scanning archives (%d / %d)", i, goal);
        printMessage(msg);
    }

    private void analyzingClasses(int i) {
        String msg = String.format("Pass %d: Analyzing classes (%d / %d) - %02d%% complete", pass + 1, i, goal, (i * 100) / goal);
        printMessage(msg);
    }

    private void printMessage(String msg) {
        if (msg.length() > 79) {
            msg = msg.substring(0, 79);
        }
        out.print("\r" + msg);
    }

    @Override
    public void startArchive(String name) {
        // noop
    }

}
