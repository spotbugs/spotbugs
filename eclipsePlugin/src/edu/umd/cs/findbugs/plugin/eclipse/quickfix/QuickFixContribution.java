/*
 * Contributions to FindBugs
 * Copyright (C) 2014, Andrey Loskutov
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

public class QuickFixContribution {

    final String clazzFqn;

    final String label;

    final String pattern;

    final Callable<? extends BugResolution> producer;

    final Map<String, String> args;

    public QuickFixContribution(@Nonnull String clazzFqn, @Nonnull String label, @Nonnull String pattern,
            @Nonnull Set<String> args, @Nonnull Callable<BugResolution> producer) {
        this.clazzFqn = clazzFqn;
        this.label = label;
        this.pattern = pattern;
        this.args = convertToMap(args);
        this.producer = producer;
    }

    private static Map<String, String> convertToMap(Set<String> args) {
        Map<String, String> params = new HashMap<String, String>();
        for (String keyValue : args) {
            String[] keyValueArr = keyValue.split("\\s*=\\s*");
            if(keyValueArr.length > 1) {
                params.put(keyValueArr[0], keyValueArr[1]);
            }
        }
        return params.size() == 0? Collections.EMPTY_MAP : Collections.unmodifiableMap(params);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + clazzFqn.hashCode();
        result = prime * result + label.hashCode();
        result = prime * result + pattern.hashCode();
        result = prime * result + args.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("QuickFixContribution [clazzFqn=").append(clazzFqn);
        builder.append(", label=").append(label).append(", pattern=");
        builder.append(pattern).append(", args=").append(args).append("]");
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof QuickFixContribution)) {
            return false;
        }
        QuickFixContribution other = (QuickFixContribution) obj;
        if (!clazzFqn.equals(other.clazzFqn)) {
            return false;
        }
        if (!label.equals(other.label)) {
            return false;
        }
        if (!pattern.equals(other.pattern)) {
            return false;
        }
        if (!args.equals(other.args)) {
            return false;
        }
        return true;
    }

}
