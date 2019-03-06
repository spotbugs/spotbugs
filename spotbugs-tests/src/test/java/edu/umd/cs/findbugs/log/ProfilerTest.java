/*
 * Contributions to SpotBugs
 * Copyright (C) 2019, kengo
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
package edu.umd.cs.findbugs.log;


import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.Comparator;

import org.junit.Test;

public class ProfilerTest {
    private Profiler createProfiler() {
        return new Profiler() {
            @Override
            Profile getProfile(Class<?> c) {
                Profile p = new Profile(c.getName());
                p.handleCall(10, null);
                if (c == String.class) {
                    p.handleCall(100, null);
                }
                return p;
            }
        };
    }

    @Test
    public void testClassNameComparator() {
        Comparator<Class<?>> c = new Profiler.ClassNameComparator(null);
        int result = c.compare(String.class, Object.class);
        assertThat(Integer.valueOf(result), is(greaterThan(0)));
    }

    @Test
    public void testTotalTimeComparator() {
        Comparator<Class<?>> c = new Profiler.TotalTimeComparator(createProfiler());
        int result = c.compare(String.class, Object.class);
        assertThat(Integer.valueOf(result), is(lessThan(0)));
    }

    @Test
    public void testTimePerCallComparator() {
        Comparator<Class<?>> c = new Profiler.TimePerCallComparator(createProfiler());
        int result = c.compare(String.class, Object.class);
        assertThat(Integer.valueOf(result), is(lessThan(0)));
    }

    @Test
    public void testTotalCallsComparator() {
        Comparator<Class<?>> c = new Profiler.TotalCallsComparator(createProfiler());
        int result = c.compare(String.class, Object.class);
        assertThat(Integer.valueOf(result), is(lessThan(0)));
    }

}
