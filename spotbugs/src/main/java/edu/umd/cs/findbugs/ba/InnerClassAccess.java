/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import javax.annotation.Nonnull;

public class InnerClassAccess {
    private final String methodName;

    private final String methodSig;

    private @Nonnull
    final XField field;

    private final boolean isLoad;

    public InnerClassAccess(String methodName, String methodSig, @Nonnull XField field, boolean isLoad) {
        this.methodName = methodName;
        this.methodSig = methodSig;
        this.field = field;
        this.isLoad = isLoad;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSig;
    }

    public @Nonnull XField getField() {
        return field;
    }

    public boolean isStatic() {
        return field.isStatic();
    }

    public boolean isLoad() {
        return isLoad;
    }
}

