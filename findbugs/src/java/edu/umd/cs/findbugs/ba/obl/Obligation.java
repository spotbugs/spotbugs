/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.obl;

import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * An obligation that must be cleaned up by error handling code. Examples
 * include open streams and database connections.
 *
 * <p>
 * See Weimer and Necula, <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>, OOPSLA 2004.
 * </p>
 *
 * @author David Hovemeyer
 */
public class Obligation {
    private final @DottedClassName String className;

    private final ObjectType type;

    private final int id;

    private boolean userObligationType;

    public Obligation(@DottedClassName  String className, int id) {
        this.className = className;
        this.type = ObjectTypeFactory.getInstance(className);
        this.id = id;
    }

    public @DottedClassName String getClassName() {
        return className;
    }

    public ObjectType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public boolean isUserObligationType() {
        return userObligationType;
    }

    public void setUserObligationType(boolean userObligationType) {
        this.userObligationType = userObligationType;
    }

    @Override
    public String toString() {
        // Make dataflow output more compact by dropping package
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
}

