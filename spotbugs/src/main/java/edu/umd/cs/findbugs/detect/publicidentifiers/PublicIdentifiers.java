/*
 * SpotBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.detect.publicidentifiers;

import java.util.HashSet;
import java.util.Set;

/**
 * This class contains all the public identifiers that are part of the
 * Java Standard Library. It is used to detect any shadowing or obscuring
 * of these identifiers.
 * Since there are so many identifiers, they are broken up into separate
 * classes, roughly the same size to avoid compiler limits.
 */
public class PublicIdentifiers {
    private Set<String> publicIdentifiers = null;

    public Set<String> getPublicIdentifiers() {
        if (publicIdentifiers == null) {
            publicIdentifiers = new HashSet<>();
            publicIdentifiers.addAll(LoadPublicIdentifiersAB.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersC.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersDE.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersFH.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersIJ.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersKM.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersNR.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersS.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersTU.getPublicIdentifiers());
            publicIdentifiers.addAll(LoadPublicIdentifiersVZ.getPublicIdentifiers());
        }
        return publicIdentifiers;
    }
}
