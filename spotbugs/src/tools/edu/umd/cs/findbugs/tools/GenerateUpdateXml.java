/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.tools;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Plugin;

/**
 * @author pugh
 */
public class GenerateUpdateXml {

    public static void main(String args[]) {
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection dfc = DetectorFactoryCollection.instance();
        for(Plugin p : dfc.plugins()) {
            System.out.println(p.getPluginId());
            System.out.println(p.getReleaseDate());
            System.out.println(p.getVersion());
            System.out.println();

        }

    }

}
