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

package edu.umd.cs.findbugs.classfile;

/**
 * @author David Hovemeyer
 */
public class InvalidClassFileFormatException extends CheckedAnalysisException {
    private final ClassDescriptor classDescriptor;

    private final ICodeBaseEntry codeBaseEntry;

    public InvalidClassFileFormatException(ClassDescriptor classDescriptor, ICodeBaseEntry codeBaseEntry) {
        super("Invalid classfile format");
        this.classDescriptor = classDescriptor;
        this.codeBaseEntry = codeBaseEntry;
    }

    public InvalidClassFileFormatException(ClassDescriptor classDescriptor, ICodeBaseEntry codeBaseEntry, Throwable cause) {
        super("Invalid classfile format", cause);
        this.classDescriptor = classDescriptor;
        this.codeBaseEntry = codeBaseEntry;
    }

    public InvalidClassFileFormatException(String msg, ClassDescriptor classDescriptor, ICodeBaseEntry codeBaseEntry) {
        super(msg);
        this.classDescriptor = classDescriptor;
        this.codeBaseEntry = codeBaseEntry;
    }

    /**
     * @return Returns the ClassDescriptor of the class found to have an invalid
     *         format (null if unknown)
     */
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    /**
     * @return Returns the codeBaseEntry.
     */
    public ICodeBaseEntry getCodeBaseEntry() {
        return codeBaseEntry;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        return super.getMessage() + " in " + codeBaseEntry;
    }
}
