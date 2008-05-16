/*
 * FindBugs - Find bugs in Java programs
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
package edu.umd.cs.findbugs;

import java.io.IOException;

import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Utility methods for BugAnnotation classes.
 * 
 * @author David Hovemeyer
 */
public abstract class BugAnnotationUtil {
	/**
	 * Write a BugAnnotation as XML.
	 * 
	 * @param xmlOutput     the XMLOutput
	 * @param elementName   name of element for BugAnnotation
	 * @param annotation    the BugAnnotation
	 * @param attributeList the XML attribute list
	 * @param addMessages   true if descriptive messages should be added
	 * @throws IOException
	 */
	public static void writeXML(
			XMLOutput xmlOutput,
			String elementName,
			BugAnnotation annotation,
			XMLAttributeList attributeList,
			boolean addMessages) throws IOException {

		if (addMessages) {
			xmlOutput.openTag(elementName, attributeList);
			if (annotation instanceof BugAnnotationWithSourceLines) {
				SourceLineAnnotation src = ((BugAnnotationWithSourceLines) annotation).getSourceLines();
				if (src != null)
					src.writeXML(xmlOutput, addMessages, false);
				
			}
			xmlOutput.openTag(BugAnnotation.MESSAGE_TAG);
			xmlOutput.writeText(annotation.toString());
			xmlOutput.closeTag(BugAnnotation.MESSAGE_TAG);
			xmlOutput.closeTag(elementName);
		} else {
			xmlOutput.openCloseTag(elementName, attributeList);
		}		

	}
}
