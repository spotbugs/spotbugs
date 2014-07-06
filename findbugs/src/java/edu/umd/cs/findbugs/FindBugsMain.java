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

package edu.umd.cs.findbugs;

import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author pugh
 */
@ParametersAreNonnullByDefault
public class FindBugsMain implements Comparable<FindBugsMain> {

    public FindBugsMain(Class<?> mainClass, String cmd, String description, String kind, boolean analysis)
            throws SecurityException, NoSuchMethodException {
        this.mainClass = mainClass;
        mainMethod = mainClass.getMethod("main", String[].class);
        this.cmd = cmd;
        this.description = description;
        this.kind = kind;
        this.analysis = analysis;
    }

    final @Nonnull
    Class<?> mainClass;

    final Method mainMethod;

    public final @Nonnull
    String cmd;

    public final String description;

    public final @Nonnull
    String kind;

    final boolean analysis;

    public void invoke(String[] args) throws Exception {
        if (!analysis) {
            FindBugs.setNoAnalysis();
        }
        mainMethod.invoke(null, (Object) args);
    }

    @Override
    public int compareTo(FindBugsMain that) {
        int result = kind.compareTo(that.kind);
        if (result != 0) {
            return result;
        }
        return cmd.compareTo(that.cmd);

    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FindBugsMain)) {
            return false;
        }
        FindBugsMain that = (FindBugsMain) o;
        return kind.equals(that.kind) && cmd.equals(that.cmd);
    }

    @Override
    public int hashCode() {
        return kind.hashCode() + cmd.hashCode();
    }

}
