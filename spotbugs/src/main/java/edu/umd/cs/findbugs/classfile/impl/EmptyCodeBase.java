/*
 * Contributions to SpotBugs
 * Copyright (C) 2018, Brian Riehman
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

package edu.umd.cs.findbugs.classfile.impl;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import java.util.NoSuchElementException;

public class EmptyCodeBase extends AbstractScannableCodeBase {

    public EmptyCodeBase(ICodeBaseLocator codeBaseLocator) {
        super(codeBaseLocator);
    }

    @Override
    public ICodeBaseIterator iterator() throws InterruptedException {
        return new ICodeBaseIterator() {
            @Override
            public boolean hasNext() throws InterruptedException {
                return false;
            }

            @Override
            public ICodeBaseEntry next() throws InterruptedException {
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public ICodeBaseEntry lookupResource(String resourceName) {
        return null;
    }

    @Override
    public String getPathName() {
        return null;
    }

    @Override
    public void close() {
    }
}
