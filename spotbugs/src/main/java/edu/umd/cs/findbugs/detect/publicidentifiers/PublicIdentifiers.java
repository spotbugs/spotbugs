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
    public static final Set<String> PUBLIC_IDENTIFIERS;

    static {
        PUBLIC_IDENTIFIERS = new HashSet<>();
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersAB.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersC.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersDE.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersFH.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersIJ.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersKM.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersNR.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersS.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersTU.getPublicIdentifiers());
        PUBLIC_IDENTIFIERS.addAll(LoadPublicIdentifiersVZ.getPublicIdentifiers());
    }
}
