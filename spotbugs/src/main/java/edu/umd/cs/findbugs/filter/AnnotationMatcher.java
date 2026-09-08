/*
 * SpotBugs - Find Bugs in Java programs
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

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.PackageMemberAnnotation;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class AnnotationMatcher implements Matcher {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationMatcher.class);

    private final NameMatch annotationName;

    @Override
    public String toString() {
        return "Annotation(name=\"" + annotationName.getValue() + "\")";
    }

    public AnnotationMatcher(String annotationName) {
        this.annotationName = new NameMatch(annotationName);
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        for (BugAnnotation bugAnnotation : bugInstance.getAnnotations()) {
            if (!(bugAnnotation instanceof PackageMemberAnnotation)) {
                continue;
            }
            PackageMemberAnnotation pma = (PackageMemberAnnotation) bugAnnotation;
            for (String javaAnnotationName : pma.getJavaAnnotationNames()) {
                if (annotationName.match(javaAnnotationName)) {
                    LOG.debug("Matched {} in {} with {}", javaAnnotationName, pma.getClassName(), annotationName);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        XMLAttributeList attributes = new XMLAttributeList().addAttribute("name", annotationName.getSpec());
        if (disabled) {
            attributes.addAttribute("disabled", "true");
        }
        xmlOutput.openCloseTag("Annotation", attributes);
    }
}
