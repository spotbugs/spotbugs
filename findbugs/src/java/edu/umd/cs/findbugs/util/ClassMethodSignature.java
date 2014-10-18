/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.util;

import javax.annotation.concurrent.Immutable;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * Class to easily match the called method
 * Objects of this class can be used as keys in HashSet/HashMap
 * @author Tagir Valeev
 */
@Immutable
public class ClassMethodSignature {
    private final @DottedClassName String className;
    private final String methodName;
    private final String signature;

    /**
     * @param className
     * @param methodName
     * @param signature
     */
    public ClassMethodSignature(@DottedClassName String className, String methodName, String signature) {
        super();
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
    }

    public ClassMethodSignature(InvokeInstruction iins, ConstantPoolGen cpg) {
        this(iins.getClassName(cpg), iins.getMethodName(cpg), iins.getSignature(cpg));
    }

    public @DottedClassName String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ClassMethodSignature other = (ClassMethodSignature) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (methodName == null) {
            if (other.methodName != null) {
                return false;
            }
        } else if (!methodName.equals(other.methodName)) {
            return false;
        }
        if (signature == null) {
            if (other.signature != null) {
                return false;
            }
        } else if (!signature.equals(other.signature)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        result = prime * result + ((signature == null) ? 0 : signature.hashCode());
        return result;
    }
}
