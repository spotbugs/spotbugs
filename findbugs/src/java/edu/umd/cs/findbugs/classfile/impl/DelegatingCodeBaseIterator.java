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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.classfile.impl;

import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;

/**
 * An implementation of ICodeBaseIterator that delegates to
 * another codebase.  In particular, the codebase entries
 * it creates are DelegatingCodeBaseEntry objects.
 * 
 * @author David Hovemeyer
 */
public class DelegatingCodeBaseIterator implements ICodeBaseIterator {
	private ICodeBase frontEndCodeBase;
	private ICodeBaseIterator delegateCodeBaseIterator;

	public DelegatingCodeBaseIterator(ICodeBase frontEndCodeBase, IScannableCodeBase delegateCodeBase) throws InterruptedException {
		this.frontEndCodeBase = frontEndCodeBase;
		this.delegateCodeBaseIterator = delegateCodeBase.iterator();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#hasNext()
	 */
	public boolean hasNext() throws InterruptedException {
		return delegateCodeBaseIterator.hasNext();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#next()
	 */
	public ICodeBaseEntry next() throws InterruptedException {
		ICodeBaseEntry delegateCodeBaseEntry = delegateCodeBaseIterator.next();
		return new DelegatingCodeBaseEntry(frontEndCodeBase, delegateCodeBaseEntry);
	}

}
