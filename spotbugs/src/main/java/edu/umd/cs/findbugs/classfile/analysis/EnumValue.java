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

package edu.umd.cs.findbugs.classfile.analysis;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

/**
 * Represents an enumeration value used with an application of an annotation.
 *
 * @author William Pugh
 * @author David Hovemeyer
 */
public class EnumValue {
    public final ClassDescriptor desc;

    public final String value;

    public EnumValue(String desc, String value) {
        this.desc = DescriptorFactory.createClassDescriptorFromSignature(desc);
        this.value = value;
    }

    @Override
    public String toString() {
        return desc.getDottedClassName() + "." + value;
    }

    @Override
    public int hashCode() {
        return desc.hashCode() + 37 * value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof EnumValue)) {
            return false;
        }
        EnumValue other = (EnumValue) obj;
        return this.desc.equals(other.desc) && this.value.equals(other.value);
    }

}
