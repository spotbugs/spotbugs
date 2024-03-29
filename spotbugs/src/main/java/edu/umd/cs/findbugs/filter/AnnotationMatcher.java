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

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
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
        // Match on the pure filename first
        ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
        if (primaryClassAnnotation == null) {
            return false;
        }
        String bugClassName = primaryClassAnnotation.getSourceFileName();
        List<String> javaAnnotationNames = primaryClassAnnotation.getJavaAnnotationNames();
        boolean matchesAny = false;
        for (String javaAnnotationName : javaAnnotationNames) {
            boolean result = annotationName.match(javaAnnotationName);
            LOG.debug("Matching {} in {} with {}, result = {}", javaAnnotationName, bugClassName, annotationName, result);
            matchesAny = matchesAny || result;
        }
        return matchesAny;
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
