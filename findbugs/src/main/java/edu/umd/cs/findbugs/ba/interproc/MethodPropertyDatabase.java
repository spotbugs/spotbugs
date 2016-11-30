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
package edu.umd.cs.findbugs.ba.interproc;

import java.io.IOException;
import java.io.Writer;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * A MethodPropertyDatabase keeps track of properties of methods. This is useful
 * for implementing interprocedural analyses.
 *
 * @author David Hovemeyer
 */
public abstract class MethodPropertyDatabase<Property> extends PropertyDatabase<MethodDescriptor, Property> {

    @Override
    protected MethodDescriptor parseKey(String methodStr) throws PropertyDatabaseFormatException {
        String[] tuple = methodStr.split(",");
        if (tuple.length != 4) {
            throw new PropertyDatabaseFormatException("Invalid method tuple: " + methodStr);
        }

        try {
            int accessFlags = Integer.parseInt(tuple[3]);
            // return
            // XFactory.createMethodDescriptor(XFactory.canonicalizeString(tuple[0]),
            // XFactory.canonicalizeString( tuple[1]),
            // XFactory.canonicalizeString(tuple[2]), accessFlags);
            String className = XFactory.canonicalizeString(tuple[0]);
            String methodName = XFactory.canonicalizeString(tuple[1]);
            String methodSig = XFactory.canonicalizeString(tuple[2]);
            return DescriptorFactory.instance().getMethodDescriptor(ClassName.toSlashedClassName(className), methodName,
                    methodSig, (accessFlags & Constants.ACC_STATIC) != 0);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void writeKey(Writer writer, MethodDescriptor method) throws IOException {
        writer.write(method.getClassDescriptor().toDottedClassName());
        writer.write(",");
        writer.write(method.getName());
        writer.write(",");
        writer.write(method.getSignature());
        writer.write(",");
        XMethod xMethod =  XFactory.createXMethod(method);
        writer.write(Integer.toString(xMethod.getAccessFlags() & 0xf));
    }
}
