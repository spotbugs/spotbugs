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

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * class to hold the user annotation and user designation for a BugInstance
 */
public class BugDesignation implements XMLWriteable, Serializable, Comparable<BugDesignation> {

	/** The default key for the user designation.
	 *  Bad things could happen if this key isn't in getUserDesignations() */
	public static final String UNCLASSIFIED = UserDesignation.UNCLASSIFIED.name();


	/** user designation -- value should be one of the keys
	 *  returned by I18N.getInstance().getUserDesignations() */
	@NonNull private String designation = UNCLASSIFIED;

	
	
	@Override
    public String toString() {
		String result =  designation;
		if (user != null)
			result += " by " + user;
		if (annotationText != null && annotationText.length() > 0)
			result += " : " + annotationText;
		return result;
	}
	private boolean dirty;
	
	public boolean isDirty() {
		return dirty;
	}
	public void cleanDirty() {
		dirty = false;
	}
	private @javax.annotation.CheckForNull String user;

	
	public BugDesignation() {}
	
	
	/**
     * @param designation
     * @param timestamp
     * @param annotationText
     * @param user
     */
    public BugDesignation(String designation, long timestamp, String annotationText, String user) {
	    this.designation = designation;
	    this.timestamp = timestamp;
	    this.annotationText = annotationText;
	    this.user = user;
    }

    public BugDesignation(BugDesignation that) {
	    this(that.designation, that.timestamp, that.annotationText, that.user);
    }
	private long timestamp = System.currentTimeMillis();

	/** free text from the user */
	//TODO: make this @CheckForNull 
	private String annotationText;

	/** return the user designation
	 *  E.g., "MOSTLY_HARMLESS", "CRITICAL", "NOT_A_BUG", etc.
	 *  
	 *  Note that this is the key, suitable for writing to XML,
	 *  but not for showing to the user.
	 *  @see I18N#getUserDesignation(String key) */
	@NonNull public String getDesignationKey() {
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
	 *  @see I18N#getUserDesignationKeys() */
	public void setDesignationKey(String designationKey) {
		if (designation.equals(designationKey)) 
			return;
		dirty = true;
		timestamp = System.currentTimeMillis();
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
		if (timestamp != ts) {
			timestamp = ts;
			dirty = true;
		}
	}

	@CheckForNull public String getAnnotationText() {
			return annotationText;
	}
	@Nonnull public String getNonnullAnnotationText() {
		if (annotationText == null) 
			return "";
		return annotationText;
}
	public void setAnnotationText(String s) {
		if (s.equals(annotationText))
			return;
		dirty = true;
		annotationText = s;
		timestamp = System.currentTimeMillis();
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

	/** replace unset fields of this user designation with values set in the other */
	public void merge(@CheckForNull BugDesignation other) {
		if (other == null) return;
		boolean changed = false;
		if ( (annotationText==null || annotationText.length()==0)
				&& other.annotationText!=null && other.annotationText.length()>0) {
			annotationText = other.annotationText;
			dirty = true;
			changed = true;
		}
		if ( (designation==null || UNCLASSIFIED.equals(designation) || designation.length()==0)
				&& other.designation!=null && other.designation.length()>0) {
			designation = other.designation;
			dirty = true;
			changed = true;
		}
		if (!changed) return; // if no changes don't even try to copy user or timestamp

		if ( (user==null || user.length()==0) && other.user!=null && other.user.length()>0) {
			user = other.user;
		}
		if (timestamp==0 && other.timestamp!=0) {
			timestamp = other.timestamp;
		}
	}

	@Override
    public int hashCode() {
		int hash = (int) this.timestamp;
		if (user != null)
			hash += user.hashCode();
		if (designation != null)
			hash += designation.hashCode();
		if (annotationText != null)
			hash += annotationText.hashCode();
		return hash;
	}
	
	 @Override
    public boolean equals(Object o) {
		 if (!(o instanceof BugDesignation))
			 return false;
		 return this.compareTo((BugDesignation)o) == 0;
	 }
    public int compareTo(BugDesignation o) {
    	if (this == o)
    		return 0;
	    int result = - Util.compare(this.timestamp, o.timestamp);
	    if (result != 0) 
	    	return result;
	   
	    result = Util.nullSafeCompareTo(this.user, o.user);
	    if (result != 0) 
	    		return result;
	    
	    result = Util.nullSafeCompareTo(this.designation, o.designation);
	    if (result != 0) 
	    		return result;
	    result = Util.nullSafeCompareTo(this.annotationText, o.annotationText);
	    if (result != 0) 
	    		return result;
	  
	    return 0;
	    
    }

}
