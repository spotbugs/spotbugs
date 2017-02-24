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

package edu.umd.cs.findbugs;

/**
 * Class for generating InstructionScanners at particular instructions of a
 * path. Because we don't know a priori where we might want to start scanning in
 * order to find a pattern in a path, we use this interface to tell us which
 * locations are candidates for starting a pattern.
 */
public interface InstructionScannerGenerator {
    /**
     * Return true if a new scanner should be created starting at this
     * instruction, false otherwise.
     */
    public boolean start(org.apache.bcel.generic.InstructionHandle handle);

    /**
     * Create a new scanner.
     */
    public InstructionScanner createScanner();
}

