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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLWriteable;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * class to hold the user annotation and user designation for a BugInstance
 */
public class BugDesignation implements XMLWriteable, Serializable {

	/** The default key for the user designation.
	 *  Bad things could happen if this key isn't in getUserDesignations() */
	public static final String UNCLASSIFIED = "UNCLASSIFIED";


	/** user designation -- value should be one of the keys
	 *  returned by I18N.getInstance().getUserDesignations() */
	@NonNull private String designation = UNCLASSIFIED;

	@CheckForNull private String user;

	private long timestamp;

	/** free text from the user */
	@CheckForNull private String annotationText;
	
	/** return the user designation
	 *  E.g., "MOSTLY_HARMLESS", "CRITICAL", "NOT_A_BUG", etc.
	 *  
	 *  Note that this is the key, suitable for writing to XML,
	 *  but not for showing to the user.
	 *  @see I18N#getUserDesignation(String key) */
    @NonNull public String getDesignation() {
		return designation;
    }

	/** set the user designation
	 *  E.g., "MOSTLY_HARMLESS", "CRITICAL", "NOT_A_BUG", etc.
	 *  
	 *  If the argument is null, it will be treated as UNCLASSIFIED.
	 *  
	 *  Note that this is the key, suitable for writing to XML,
	 *  but not what the user sees. Strange things could happen
	 *  if designationKey is not one of the keys returned by
	 *  I18N.instance().getUserDesignations().
	 *  @see I18N#getUserDesignations() */
    public void setDesignation(String designationKey) {
		designation = (designationKey!=null ? designationKey : UNCLASSIFIED);
    }


    @CheckForNull public String getUser() {
		return user;
    }
    public void setUser(String u) {
		user = u;
    }

    public long getTimestamp() {
    		return timestamp;
    }
    public void setTimestamp(long ts) {
    		timestamp = ts;
    }

    @CheckForNull public String getAnnotationText() {
    		return annotationText;
    }
    public void setAnnotationText(String s) {
    		annotationText = s;
    }

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList();
		// all three of these xml attributes are optional
		if (designation != null && !UNCLASSIFIED.equals(designation))
			attributeList.addAttribute("designation", designation);
		if (user != null && !"".equals(user))
			attributeList.addAttribute("user", user);
		if (timestamp > 0)
			attributeList.addAttribute("timestamp", String.valueOf(timestamp));

		if ((annotationText != null && !"".equals(annotationText))) {
			xmlOutput.openTag("UserAnnotation", attributeList);
			xmlOutput.writeCDATA(annotationText);
			xmlOutput.closeTag("UserAnnotation");
		} else {
			xmlOutput.openCloseTag("UserAnnotation", attributeList);
		}
	}

}
