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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.log.Profiler.Profile;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * <p>
 * A class that summarize profile recorded by multiple {@link Profiler} instances. SpotBugs uses this class to summarize
 * profiles from all worker threads.
 * </p>
 *
 * @since 4.0
 */
public class ProfileSummary implements IProfiler, XMLWriteable {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ProfileSummary.class);
    private final Profiler[] profilers;

    public ProfileSummary(@NonNull Profiler... profilers) {
        this.profilers = Objects.requireNonNull(profilers);
    }

    /**
     * <p>
     * Report summarized profile to given {@link PrintStream}.
     * </p>
     * <p>
     * This method does not check the state of given {@link PrintStream}, and it is {@literal caller's} duty to check it by
     * {@link PrintStream#checkError()}.
     * </p>
     *
     * @param reportComparator
     * @param filter
     * @param stream
     */
    public void report(Comparator<Class<?>> reportComparator, Predicate<Profile> filter, PrintStream stream) {
        stream.println("PROFILE REPORT");
        try {
            TreeSet<Class<?>> treeSet = Arrays.stream(profilers)
                    .map(Profiler::getTargetClasses)
                    .flatMap(Set::stream)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(reportComparator)));
            stream.printf("%8s  %8s %9s %s%n", "msecs", "#calls", "usecs/call", "Class");

            for (Class<?> c : treeSet) {
                Profile p = getProfile(c);
                if (filter.test(p)) {
                    long time = p.totalTime.get();
                    int callCount = p.totalCalls.get();
                    stream.printf("%8d  %8d  %8d %s%n", Long.valueOf(TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS)),
                            Integer.valueOf(callCount),
                            Long.valueOf(TimeUnit.MICROSECONDS.convert(time / callCount, TimeUnit.NANOSECONDS)),
                            c.getSimpleName());
                }

            }
            stream.flush();
        } catch (RuntimeException e) {
            // FIXME Catching RuntimeException just to keep compatibility
            LOG.error("Failed to print profile report.", e);
        }
    }

    @Override
    public void writeXML(@NonNull XMLOutput xmlOutput) throws IOException {
        xmlOutput.startTag("FindBugsProfile");
        xmlOutput.stopTag(false);
        Comparator<Class<?>> reportComparator = new Profiler.TotalTimeComparator(this);
        TreeSet<Class<?>> treeSet = Arrays.stream(profilers)
                .map(Profiler::getTargetClasses)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(() -> new TreeSet<>(reportComparator)));

        long totalTime = Arrays.stream(profilers)
                .map(Profiler::getProfiles)
                .flatMap(Collection::stream)
                .mapToLong(Profiler.Profile::getTotalTime)
                .sum();

        long accumulatedTime = 0;

        for (Class<?> c : treeSet) {
            Profile p = getProfile(c);
            p.writeXML(xmlOutput);
            accumulatedTime += p.getTotalTime();
            if (accumulatedTime > 3 * totalTime / 4) {
                break;
            }
        }
        xmlOutput.closeTag("FindBugsProfile");
    }

    @Override
    public Profile getProfile(Class<?> clazz) {
        return Arrays.stream(profilers)
                .filter(profiler -> profiler.contains(clazz))
                .map(profiler -> profiler.getProfile(clazz))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Specified class " + clazz + " is not analyzed"));
    }
}
