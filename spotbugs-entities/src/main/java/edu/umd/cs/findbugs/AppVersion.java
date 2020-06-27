/*
 * FindBugs - Find Bugs in Java programs
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
import java.util.Date;

import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * A version of an analyzed application. Application versions are uniquely
 * identified by a sequence number, which represents a run of FindBugs on the
 * application. Timestamp is when FindBugs was run (according to
 * System.currentTimeMillis()), and the release name is available if the user
 * provided it.
 *
 * @author David Hovemeyer
 */
public class AppVersion implements XMLWriteable, Cloneable {
    /**
     * XML element name for a stored AppVersion object.
     */
    public static final String ELEMENT_NAME = "AppVersion";

    private final long sequence;

    private long timestamp;

    private String releaseName;

    private int numClasses;

    private int codeSize;

    public AppVersion(long sequence, long time, String name) {
        this.sequence = sequence;
        this.timestamp = time;
        this.releaseName = name;
    }

    public AppVersion(long sequence, Date time, String name) {
        this.sequence = sequence;
        this.timestamp = time.getTime();
        this.releaseName = name;
    }

    public AppVersion(long sequence) {
        this.sequence = sequence;
        this.timestamp = System.currentTimeMillis();
        this.releaseName = "";
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#clone()
     */

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @return Returns the sequence.
     */
    public long getSequenceNumber() {
        return sequence;
    }

    /**
     * @return Returns the timestamp.
     */
    public long getTimestamp() {
        if (timestamp <= 0) {
            return System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * @return Returns the releaseName.
     */
    public String getReleaseName() {
        return releaseName;
    }

    /**
     * @param timestamp
     *            The timestamp to set.
     */
    public AppVersion setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * @param releaseName
     *            The releaseName to set.
     */
    public AppVersion setReleaseName(String releaseName) {
        this.releaseName = releaseName;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.xml.XMLWriteable#writeXML(edu.umd.cs.findbugs.xml
     * .XMLOutput)
     */
    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        xmlOutput.openCloseTag(
                ELEMENT_NAME,
                new XMLAttributeList().addAttribute("sequence", String.valueOf(sequence))
                        .addAttribute("timestamp", String.valueOf(timestamp)).addAttribute("release", releaseName)
                        .addAttribute("codeSize", String.valueOf(codeSize))
                        .addAttribute("numClasses", String.valueOf(numClasses)));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.valueOf(sequence));
        buf.append(',');
        buf.append(String.valueOf(timestamp));
        buf.append(',');
        buf.append(releaseName);
        buf.append(',');
        buf.append(codeSize);
        buf.append(',');
        buf.append(codeSize);
        return buf.toString();
    }

    /**
     * @param numClasses
     *            The numClasses to set.
     */
    public AppVersion setNumClasses(int numClasses) {
        this.numClasses = numClasses;
        return this;
    }

    /**
     * @return Returns the numClasses.
     */
    public int getNumClasses() {
        return numClasses;
    }

    /**
     * @param codeSize
     *            The codeSize to set.
     */
    public AppVersion setCodeSize(int codeSize) {
        this.codeSize = codeSize;
        return this;
    }

    /**
     * @return Returns the codeSize.
     */
    public int getCodeSize() {
        return codeSize;
    }
}
