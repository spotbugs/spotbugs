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

package edu.umd.cs.findbugs.filter;

import java.io.IOException;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * @author rafal@caltha.pl
 */
public class FieldMatcher extends MemberMatcher implements Matcher {

    public FieldMatcher(String name) {
        super(name);
    }

    public FieldMatcher(String name, String type) {
        this(name, SignatureUtil.createFieldSignature(type), null);
    }

    public FieldMatcher(String name, String type, String role) {
        super(name, SignatureUtil.createFieldSignature(type), role);
    }

    @Override
    public String toString() {
        return "Method(" + super.toString() + ")";
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        FieldAnnotation fieldAnnotation = null;
        if (role == null || "".equals(role)) {
            fieldAnnotation = bugInstance.getPrimaryField();
        } else {
            for (BugAnnotation a : bugInstance.getAnnotations()) {
                if (a instanceof FieldAnnotation && role.equals(a.getDescription())) {
                    fieldAnnotation = (FieldAnnotation) a;
                    break;
                }
            }
        }
        if (fieldAnnotation == null) {
            return false;
        }
        if (!name.match(fieldAnnotation.getFieldName())) {
            return false;
        }
        if (signature != null && !signature.match(fieldAnnotation.getFieldSignature())) {
            return false;
        }
        return true;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        XMLAttributeList attributes = new XMLAttributeList().addAttribute("name", name.getSpec());
        if (signature != null) {
            attributes.addOptionalAttribute("signature", signature.getSpec());
        }
        attributes.addOptionalAttribute("role", role);
        if (disabled) {
            attributes.addAttribute("disabled", "true");
        }
        xmlOutput.openCloseTag("Field", attributes);
    }
}
