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

package edu.umd.cs.findbugs.classfile.engine.asm;

import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.isNull;

import org.objectweb.asm.Opcodes;

/**
 * @author pugh
 */
public class FindBugsASM {

    private static final String EXPERIMENTAL_PROPERTY_VALUE = System.getProperty("spotbugs.experimental");
    private static final boolean USE_EXPERIMENTAL = isNull(EXPERIMENTAL_PROPERTY_VALUE) || parseBoolean(EXPERIMENTAL_PROPERTY_VALUE);

    public static final int ASM_VERSION = USE_EXPERIMENTAL ? Opcodes.ASM7_EXPERIMENTAL : Opcodes.ASM6;

}
