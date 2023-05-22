package edu.umd.cs.findbugs.anttask;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class UnionBugs2Test {
    @Test
    public void testWriteXmlFilePathArgsToTextFile() {
        //Prepare
        UnionBugs2 unionBugs2 = new UnionBugs2();
        unionBugs2.setProject(new Project());
        unionBugs2.createFindbugsEngine();
        unionBugs2.setTo("./target.xml");
        FileSet xmlFileSet = new FileSet();
        xmlFileSet.setDir(new File("./src/test/resources/"));
        xmlFileSet.setIncludes("*.xml");
        unionBugs2.addFileset(xmlFileSet);

        //Act
        unionBugs2.configureFindbugsEngine();
        CommandlineJava commandLine = unionBugs2.getFindbugsEngine().getCommandLine();

        //Verify
        String[] arguments = commandLine.getJavaCommand().getArguments();
        Optional<String> textFileArgument = Stream.of(arguments).filter(s -> s.contains("spotbugs-argument-file") && s.endsWith(".txt")).findFirst();
        Assert.assertTrue("Arguments are supposed to be compiled into text file.", textFileArgument.isPresent());
        String textFile = textFileArgument.get();
        List<String> xmlPaths = readArgumentsFile(textFile);
        Assert.assertTrue(xmlPaths.stream().anyMatch(s -> s.contains("findbugsExclusionTest.xml")));
        Assert.assertTrue(xmlPaths.stream().anyMatch(s -> s.contains("findbugsExclusionTest2.xml")));

        //Cleanup
        try {
            Files.deleteIfExists(new File(textFile).toPath());
        } catch (IOException e) {
            System.err.println("Unable to clean up after test. File " + textFile + " still exists");
        }
    }

    private static List<String> readArgumentsFile(String textFile) {
        List<String> compiledXmlFiles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(textFile, StandardCharsets.UTF_8))) {
            String next;
            while ((next = reader.readLine()) != null) {
                compiledXmlFiles.add(next);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return compiledXmlFiles;
    }
}
