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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * @since 4.0
 */
public class ProfileSummaryTest {

    @Test
    public void testGetProfile() {
        Profiler profiler = new Profiler();
        profiler.start(String.class);
        profiler.end(String.class);
        Profiler another = new Profiler();
        another.start(Object.class);
        another.end(Object.class);
        ProfileSummary summary = new ProfileSummary(profiler, another);

        assertThat(summary.getProfile(String.class), is(profiler.getProfile(String.class)));
        assertThat(summary.getProfile(Object.class), is(another.getProfile(Object.class)));
    }

    @Test
    public void testReport() throws UnsupportedEncodingException {
        Profiler profiler = new Profiler();
        profiler.start(String.class);
        profiler.end(String.class);
        Profiler another = new Profiler();
        another.start(Object.class);
        another.end(Object.class);
        ProfileSummary summary = new ProfileSummary(profiler, another);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(byteArray);

        summary.report(new Profiler.ClassNameComparator(summary), (profile) -> true, stream);
        String report = byteArray.toString(StandardCharsets.UTF_8.name());
        assertThat(report, containsString("Object"));
        assertThat(report, containsString("String"));
    }

    @Test
    public void testWriteXML() throws IOException {
        Profiler profiler = new Profiler();
        Profiler another = new Profiler();

        // The XML report contains only profiles that consumes 10ms, so add Thread.sleep(int) in test
        profiler.start(String.class);
        another.start(Object.class);
        try {
            Thread.sleep(32); // two times of Windows' timestamp resolution
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        profiler.end(String.class);
        another.end(Object.class);

        ProfileSummary summary = new ProfileSummary(profiler, another);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        XMLOutput output = new OutputStreamXMLOutput(byteArray);

        summary.writeXML(output);
        output.finish();
        String xml = byteArray.toString(StandardCharsets.UTF_8.name());
        assertThat(xml, containsString("name=\"java.lang.Object\""));
        assertThat(xml, containsString("name=\"java.lang.String\""));
    }
}
