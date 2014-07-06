/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

public abstract class AbstractField extends AbstractClassMember implements XField {

    protected AbstractField(@DottedClassName String className, String fieldName, String fieldSig, int accessFlags) {
        super(className, fieldName, fieldSig, accessFlags);
    }

    @Override
    public boolean isVolatile() {
        return (getAccessFlags() & Constants.ACC_VOLATILE) != 0;
    }

    @Override
    public final boolean isSynthetic() {
        return (getAccessFlags() & Constants.ACC_SYNTHETIC) != 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XField#getFieldDescriptor()
     */
    @Override
    public FieldDescriptor getFieldDescriptor() {
        return DescriptorFactory.instance().getFieldDescriptor(getClassDescriptor().getClassName(), getName(), getSignature(),
                isStatic());
    }
}

