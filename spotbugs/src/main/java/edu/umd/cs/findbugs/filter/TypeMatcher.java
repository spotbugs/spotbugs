/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
 *
 * Author: Andrey Loskutov
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

package edu.umd.cs.findbugs.filter;

import java.io.IOException;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class TypeMatcher implements Matcher {

    private final NameMatch descriptor;

    private final String role;

    private final String typeParameters;

    @Override
    public String toString() {
        return "Type(descriptor=\"" + descriptor.getValue() + "\")";
    }

    public TypeMatcher(String descriptor, String role, String typeParameters) {
        this.descriptor = new NameMatch(descriptor);
        this.role = role;
        this.typeParameters = typeParameters;
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        TypeAnnotation typeAnnotation = bugInstance.getPrimaryType();
        if (role != null && !"".equals(role)) {
            for (BugAnnotation a : bugInstance.getAnnotations()) {
                if (a instanceof TypeAnnotation && role.equals(a.getDescription())) {
                    typeAnnotation = (TypeAnnotation) a;
                    break;
                }
            }
        }
        if(typeAnnotation == null){
            return false;
        }
        String typeDesctiptor = typeAnnotation.getTypeDescriptor();
        if(!descriptor.match(typeDesctiptor)){
            return false;
        }
        if (typeParameters != null && !typeParameters.equals(typeAnnotation.getTypeParameters())) {
            return false;
        }
        return true;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        XMLAttributeList attributes = new XMLAttributeList().addAttribute("descriptor", descriptor.getSpec());
        if (disabled) {
            attributes.addAttribute("disabled", "true");
        }
        attributes.addOptionalAttribute("typeParameters", typeParameters);
        attributes.addOptionalAttribute("role", role);
        xmlOutput.openCloseTag("Type", attributes);
    }
}

