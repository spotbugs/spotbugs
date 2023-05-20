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
    public static Set<String> PUBLIC_IDENTIFIERS = new HashSet<>();

    static {
        LoadPublicIdentifiersAB.load();
        LoadPublicIdentifiersC.load();
        LoadPublicIdentifiersDE.load();
        LoadPublicIdentifiersFH.load();
        LoadPublicIdentifiersIJ.load();
        LoadPublicIdentifiersKM.load();
        LoadPublicIdentifiersNR.load();
        LoadPublicIdentifiersS.load();
        LoadPublicIdentifiersTU.load();
        LoadPublicIdentifiersVZ.load();
    }
}
