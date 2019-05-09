/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check that the BCEL classes present seem to be the right ones. Specifically,
 * we check whether the ones extended in FindBugs code are non-final. The
 * following BCEL classes are extended in FindBugs code:
 *
 * org.apache.bcel.generic.ObjectType; org.apache.bcel.generic.Type;
 * org.apache.bcel.Constants; org.apache.bcel.classfile.EmptyVisitor
 * org.apache.bcel.Repository;
 *
 * @author langmead
 */

public class CheckBcel {
    private static final Logger LOG = LoggerFactory.getLogger(CheckBcel.class);

    /**
     *
     */
    private static final String ORG_APACHE_BCEL_REPOSITORY = "org.apache.bcel.Repository";

    /**
     *
     */
    private static final String ORG_APACHE_BCEL_CLASSFILE_EMPTY_VISITOR = "org.apache.bcel.classfile.EmptyVisitor";

    /**
     *
     */
    private static final String ORG_APACHE_BCEL_CONSTANTS = "org.apache.bcel.Constants";

    /**
     *
     */
    private static final String ORG_APACHE_BCEL_GENERIC_TYPE = "org.apache.bcel.generic.Type";

    /**
     *
     */
    private static final String ORG_APACHE_BCEL_GENERIC_OBJECT_TYPE = "org.apache.bcel.generic.ObjectType";

    /**
     * Check whether given Class is declared final
     *
     * @param c
     *            the class to check
     * @return true iff Class is declared final
     */
    private static boolean isFinal(Class<?> c) {
        return (c.getModifiers() & Modifier.FINAL) != 0;
    }

    /**
     * Output an appropriate error when a BCEL class looks wrong.
     *
     * @param cname
     *            name of the BCEL class
     */
    private static void error(String cname) {
        LOG.error("BCEL class compatibility error.");
        LOG.error("The version of class {} found was not compatible with\n"
                + "SpotBugs.  Please remove any BCEL libraries that may be interfering.  This may happen\n"
                + "if you have an old version of BCEL or a library that includes an old version of BCEL\n"
                + "in an \"endorsed\" directory.", cname);
    }

    /**
     * Check that the BCEL classes present seem to be the right ones.
     * Specifically, we check whether the ones extended in FindBugs code are
     * non-final.
     *
     * @return true iff all checks passed
     */
    public static boolean check() {
        Class<?> objectType;
        Class<?> type;
        Class<?> constants;
        Class<?> emptyVis;
        Class<?> repository;
        try {
            objectType = Class.forName(ORG_APACHE_BCEL_GENERIC_OBJECT_TYPE);
            type = Class.forName(ORG_APACHE_BCEL_GENERIC_TYPE);
            constants = Class.forName(ORG_APACHE_BCEL_CONSTANTS);
            emptyVis = Class.forName(ORG_APACHE_BCEL_CLASSFILE_EMPTY_VISITOR);
            repository = Class.forName(ORG_APACHE_BCEL_REPOSITORY);

        } catch (ClassNotFoundException e) {
            LOG.error("One or more required BCEL classes were missing."
                    + " Ensure that bcel.jar is placed at the same directory with spotbugs.jar");
            return false;
        }
        if (isFinal(objectType)) {
            error(ORG_APACHE_BCEL_GENERIC_OBJECT_TYPE);
            return false;
        }
        if (isFinal(type)) {
            error(ORG_APACHE_BCEL_GENERIC_TYPE);
            return false;
        }
        if (isFinal(constants)) {
            error(ORG_APACHE_BCEL_CONSTANTS);
            return false;
        }
        if (isFinal(emptyVis)) {
            error(ORG_APACHE_BCEL_CLASSFILE_EMPTY_VISITOR);
            return false;
        }
        if (isFinal(repository)) {
            error(ORG_APACHE_BCEL_REPOSITORY);
            return false;
        }
        return true;
    }
}
