/*
 * Bytecode analysis framework
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

package edu.umd.cs.findbugs.ba.interproc;

import java.io.IOException;
import java.io.Writer;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Interprocedural field property database.
 *
 * @author David Hovemeyer
 */
public abstract class FieldPropertyDatabase<Property> extends PropertyDatabase<FieldDescriptor, Property> {

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.interproc.PropertyDatabase#parseKey(java.lang.
     * String)
     */
    @Override
    protected FieldDescriptor parseKey(String s) throws PropertyDatabaseFormatException {
        String[] tuple = s.split(",");
        if (tuple.length != 4) {
            throw new PropertyDatabaseFormatException("Invalid field tuple: " + s);
        }

        String className = XFactory.canonicalizeString(tuple[0]);
        String fieldName = XFactory.canonicalizeString(tuple[1]);
        String signature = XFactory.canonicalizeString(tuple[2]);
        int accessFlags;
        try {
            accessFlags = Integer.parseInt(tuple[3]);
        } catch (NumberFormatException e) {
            throw new PropertyDatabaseFormatException("Invalid field access flags: " + tuple[3]);
        }

        return DescriptorFactory.instance().getFieldDescriptor(ClassName.toSlashedClassName(className), fieldName, signature,
                (accessFlags & Constants.ACC_STATIC) != 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.interproc.PropertyDatabase#writeKey(java.io.Writer
     * , KeyType)
     */

    @Override
    protected void writeKey(Writer writer, FieldDescriptor key) throws IOException {
        writer.write(key.getClassDescriptor().getDottedClassName());
        writer.write(",");
        writer.write(key.getName());
        writer.write(",");
        writer.write(key.getSignature());
        writer.write(",");
        XField xField = XFactory.createXField(key);
        int flags = xField.getAccessFlags() & 0xf;
        writer.write(String.valueOf(flags));
    }

}
