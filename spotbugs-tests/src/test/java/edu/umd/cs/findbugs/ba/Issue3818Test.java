package edu.umd.cs.findbugs.ba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;

class Issue3818Test {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        SourceLineAnnotation.clearGenerateRelativeSource();
    }

    @Test
    void getSourcePathUsesResolvedSourceTreePath() throws Exception {
        Path srcRoot = tempDir.resolve("src");
        Files.createDirectories(srcRoot.resolve("com/example"));
        Files.writeString(srcRoot.resolve("com/example/Example.java"), "class Example {}\n");

        Project project = new Project();
        project.getSourceFinder().setSourceBaseList(Collections.singletonList(srcRoot.toString()));
        SourceLineAnnotation.generateRelativeSource(tempDir.toFile(), project);

        SourceLineAnnotation annotation = new SourceLineAnnotation("com.example.Example", "Example.java", 1, 1, 0, 0);

        assertEquals("com/example/Example.java", annotation.getSourcePath());
    }

    @Test
    void getSourcePathOmitsInventedPathForRelocatedClassName() throws Exception {
        Path srcRoot = tempDir.resolve("src");
        Files.createDirectories(srcRoot.resolve("com/example"));
        Files.writeString(srcRoot.resolve("com/example/Example.java"), "class Example {}\n");

        Project project = new Project();
        project.getSourceFinder().setSourceBaseList(Collections.singletonList(srcRoot.toString()));
        SourceLineAnnotation.generateRelativeSource(tempDir.toFile(), project);

        SourceLineAnnotation annotation = new SourceLineAnnotation("relocated.shaded.com.example.Example",
                "Example.java", 1, 1, 0, 0);

        assertNull(annotation.getSourcePath());
    }

    @Test
    void writeXmlOmitsSourcepathWhenSourceIsNotResolvable() throws Exception {
        Path srcRoot = tempDir.resolve("src");
        Files.createDirectories(srcRoot.resolve("com/example"));
        Files.writeString(srcRoot.resolve("com/example/Example.java"), "class Example {}\n");

        Project project = new Project();
        project.getSourceFinder().setSourceBaseList(Collections.singletonList(srcRoot.toString()));
        SourceLineAnnotation.generateRelativeSource(tempDir.toFile(), project);

        SourceLineAnnotation annotation = new SourceLineAnnotation("relocated.shaded.com.example.Example",
                "Example.java", 10, 10, 0, 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        annotation.writeXML(new OutputStreamXMLOutput(out), false, true);
        String xml = out.toString(StandardCharsets.UTF_8);

        assertEquals(-1, xml.indexOf("sourcepath="));
    }
}
