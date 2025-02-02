/*
 * Contributions to SpotBugs
 * Copyright (C) 2018, Brian Riehman
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

package edu.umd.cs.findbugs.classfile.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


class ClassFactoryTest {

    @BeforeEach
    void setUp() {
        Global.setAnalysisCacheForCurrentThread(new NoopAnalysisCache());
    }

    @AfterEach
    void tearDown() {
        Global.setAnalysisCacheForCurrentThread(null);
    }

    @Test
    void ignoreNonExistentFile() throws Exception {
        File file = tempFile();
        file.delete();
        FilesystemCodeBaseLocator locator = buildLocator(file);
        assertHasNoCodeBase(ClassFactory.createFilesystemCodeBase(locator));
    }

    @Test
    void ignoreUnreadableFile() throws Exception {
        File file = createZipFile();
        file.deleteOnExit();
        file.setReadable(false);
        assumeFalse(file.canRead(), "File cannot be marked as unreadable, skipping test.");
        FilesystemCodeBaseLocator locator = buildLocator(file);
        assertHasNoCodeBase(ClassFactory.createFilesystemCodeBase(locator));
    }

    @Test
    void ignoreUnknownNonClassFileFormat() throws Exception {
        FilesystemCodeBaseLocator locator = buildLocator(tempFile());
        assertHasNoCodeBase(ClassFactory.createFilesystemCodeBase(locator));
    }

    @Test
    void acceptZipFile() throws Exception {
        File zipFile = createZipFile();
        zipFile.deleteOnExit();
        FilesystemCodeBaseLocator locator = buildLocator(zipFile);
        assertHasCodeBase(ClassFactory.createFilesystemCodeBase(locator));
    }

    private static File tempFile() throws Exception {
        File tempFile = File.createTempFile("bug-497", ".txt");
        tempFile.deleteOnExit();
        return tempFile;
    }

    private static File createZipFile() throws Exception {
        File zipFile = tempFile();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
        ZipEntry entry = new ZipEntry("firstEntry");
        out.putNextEntry(entry);
        out.write("fileContents".getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
        out.close();
        return zipFile;
    }

    private FilesystemCodeBaseLocator buildLocator(File file) throws Exception {
        return new FilesystemCodeBaseLocator(file.getCanonicalPath());
    }

    private void assertHasNoCodeBase(IScannableCodeBase codeBase) throws Exception {
        assertNotNull(codeBase);
        assertFalse(codeBase.iterator().hasNext());
    }

    private void assertHasCodeBase(IScannableCodeBase codeBase) throws Exception {
        assertNotNull(codeBase);
        assertTrue(codeBase.iterator().hasNext());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../spotbugsTestCases/src/classSamples/recordCompileWithJaCoCo0.8.8/Foo.clazz",
        "../spotbugsTestCases/src/classSamples/recordCompileWithJaCoCo0.8.8/FooWithMember.clazz" })
    void acceptConstantDynamic(String fileName) throws Exception {
        try (SingleFileCodeBase codeBase = new SingleFileCodeBase(null, fileName)) {
            ClassDescriptor classDescriptor = codeBase.getClassDescriptor();

            assertNotNull(classDescriptor);
        }
    }
}
