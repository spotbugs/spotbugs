/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius
 * Copyright (C) 2005 University of Maryland
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
 * is a marker interface for detectors that don't save state from one class file
 * to the next.
 *
 * If a detector implements this interface, a clone will be generated for each
 * element it is applied to.
 *
 * The idea of using this interface is questionable. Better for people writing
 * stateless detectors to just not keep around state they don't need, rather
 * than depending on cloning and garbage collection.
 *
 */

public interface StatelessDetector extends Cloneable {
    public Object clone() throws CloneNotSupportedException;
}
