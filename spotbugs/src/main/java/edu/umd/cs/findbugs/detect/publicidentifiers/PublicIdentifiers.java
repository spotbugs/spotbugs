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
