/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flags returned in the process exit code returned when the FindBugs text UI is
 * invoked with the -exitcode command line argument. These are combined in a bit
 * set.
 */
public interface ExitCodes {
    /**
     * Serious analysis errors occurred.
     */
    static final int ERROR_FLAG = 4;

    /**
     * Classes needed for analysis were missing.
     */
    static final int MISSING_CLASS_FLAG = 2;

    /**
     * Bugs were reported.
     */
    static final int BUGS_FOUND_FLAG = 1;

    static int from(int errors, int missingClasses, int bugs) {
        Logger logger = LoggerFactory.getLogger(ExitCodes.class);
        int exitCode = 0;
        logger.debug("Calculating exit code...");
        if (errors > 0) {
            exitCode |= ERROR_FLAG;
            logger.debug("Setting 'errors encountered' flag ({})", ERROR_FLAG);
        }
        if (missingClasses > 0) {
            exitCode |= MISSING_CLASS_FLAG;
            logger.debug("Setting 'missing class' flag ({})", MISSING_CLASS_FLAG);
        }
        if (bugs > 0) {
            exitCode |= BUGS_FOUND_FLAG;
            logger.debug("Setting 'bugs found' flag ({})", BUGS_FOUND_FLAG);
        }
        logger.debug("Exit code set to: {}", exitCode);

        return exitCode;
    }
}
