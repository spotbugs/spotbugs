/*
 * XML input/output support for FindBugs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.xml;

import java.io.IOException;

/**
 * Interface indicating that an object can write itself to an XML document.
 *
 * @see XMLOutput
 * @author David Hovemeyer
 */
public interface XMLWriteable {
    /**
     * Write this object to given XMLOutput.
     *
     * @param xmlOutput
     *            the XMLOutput for the document
     */
    public void writeXML(XMLOutput xmlOutput) throws IOException;
}
