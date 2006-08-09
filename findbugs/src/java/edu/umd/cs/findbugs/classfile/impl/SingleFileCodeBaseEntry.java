package edu.umd.cs.findbugs.classfile.impl;

import java.io.IOException;
import java.io.InputStream;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Codebase entry for a single-file codebase.
 * 
 * @author David Hovemeyer
 */
public class SingleFileCodeBaseEntry implements ICodeBaseEntry {
	private final SingleFileCodeBase codeBase;

	/**
	 * Constructor.
	 * 
	 * @param codeBase parent codebase
	 */
	public SingleFileCodeBaseEntry(SingleFileCodeBase codeBase) {
		this.codeBase = codeBase;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getNumBytes()
	 */
	public int getNumBytes() {
		return codeBase.getNumBytes();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getResourceName()
	 */
	public String getResourceName() {
		return codeBase.getResourceName();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#openResource()
	 */
	public InputStream openResource() throws IOException {
		return codeBase.openFile();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getCodeBase()
	 */
	public ICodeBase getCodeBase() {
		return codeBase;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getClassDescriptor()
	 */
	public ClassDescriptor getClassDescriptor() throws ResourceNotFoundException, InvalidClassFileFormatException {
		return codeBase.getClassDescriptor();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#overrideResourceName(java.lang.String)
	 */
	public void overrideResourceName(String resourceName) {
		// FIXME: implement this
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		SingleFileCodeBaseEntry other = (SingleFileCodeBaseEntry) obj;
		return other.codeBase.equals(this.codeBase);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return codeBase.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return codeBase.getPathName();
	}
}
