/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

public abstract class AbstractMethod extends AbstractClassMember implements XMethod {

    protected AbstractMethod(@DottedClassName String className, String methodName, String methodSig, int accessFlags) {
        super(className, methodName, methodSig, accessFlags);
    }

    @Override
    public int getNumParams() {
        // FIXME: cache this?
        return new SignatureParser(getSignature()).getNumParameters();
    }

    @Override
    public boolean isNative() {
        return (getAccessFlags() & Constants.ACC_NATIVE) != 0;
    }

    @Override
    public boolean isSynchronized() {
        return (getAccessFlags() & Constants.ACC_SYNCHRONIZED) != 0;
    }

    @Override
    public boolean isBridge() {
        return (getAccessFlags() & Constants.ACC_BRIDGE) != 0;
    }

    @Override
    public String toString() {
        return SignatureConverter.convertMethodSignature(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#getMethodDescriptor()
     */
    @Override
    public MethodDescriptor getMethodDescriptor() {
        return DescriptorFactory.instance().getMethodDescriptor(ClassName.toSlashedClassName(getClassName()), getName(),
                getSignature(), isStatic());
    }

    @Override
    public XMethod resolveAccessMethodForMethod() {
        MethodDescriptor access = getAccessMethodForMethod();
        if (access != null) {
            return XFactory.createXMethod(access);
        }
        return this;

    }

}
