/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba.vna;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Global flags controlling ValueNumberAnalysis.
 */
public interface ValueNumberAnalysisFeatures {
    /**
     * When set, perform redundant load elimination and forward substitution.
     * Note that we do <em>not</em> do this in a correctness-preserving way! For
     * example, we don't kill loads when methods are called, even though those
     * methods could change heap values. The intent here is simply to try to
     * handle situations where a field is read multiple times, where the intent
     * of the programmer is clearly that the loaded values will be the same in
     * each case.
     * <p/>
     * <p>
     * Eventually, we might do interprocedural analysis that would allow
     * accurate modeling of which fields a called method could modify, which
     * would allow a more correct implementation.
     */
    public static final boolean REDUNDANT_LOAD_ELIMINATION = !SystemProperties.getBoolean("vna.noRLE");

    /**
     * Debug redundant load elimination.
     */
    public static final boolean RLE_DEBUG = SystemProperties.getBoolean("vna.rle.debug");
}

