package edu.umd.cs.findbugs.workflow;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class UnionResultsTest {

    @Test
    void testMain() throws IOException {
        //Prepare
        String fileName = createBugFile();
        File outputFile = new File("build/tmp/test/unionresults/output.xml");
        outputFile.getParentFile().mkdirs();

        //Act
        UnionResults.main(new String[] { "-withMessages", "-output", outputFile.getAbsolutePath(), fileName });

        //Verify
        List<String> output = readOutPut(outputFile.getAbsolutePath());
        Assertions.assertTrue(output.stream().anyMatch(line -> line.contains("(Lorg/test/TestClass;Ljava/util/List;)V")));
        Assertions.assertTrue(output.stream().anyMatch(line -> line.contains("(Lorg/test/TestClass2;Ljava/util/List;)V")));
    }

    private List<String> readOutPut(String absolutePath) throws IOException {
        ArrayList<String> retVal = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(absolutePath))) {
            String next;
            while ((next = reader.readLine()) != null) {
                retVal.add(next);
            }
        }
        return retVal;
    }

    private static String createBugFile() throws IOException {
        Path tempFile = Files.createTempFile("spotbugs-test", ".txt");
        String fileName = tempFile.toString();
        File firstFile = new File("build/tmp/test/unionresults/firstFile.xml");
        File secondFile = new File("build/tmp/test/unionresults/secondFile.xml");
        Files.deleteIfExists(firstFile.toPath());
        Files.deleteIfExists(secondFile.toPath());
        firstFile.getParentFile().mkdirs();
        secondFile.getParentFile().mkdirs();
        try (PrintWriter fileWriter = new PrintWriter(new FileWriter(fileName))) {
            fileWriter.write(firstFile.getAbsolutePath() + "\n");
            fileWriter.write(secondFile.getAbsolutePath());
        }

        SortedBugCollection bugInstances = new SortedBugCollection();
        BugInstance bug1 = new BugInstance("Test", 0);
        bug1.addMethod("TestClass", "TestMethod", "(Lorg/test/TestClass2;Ljava/util/List;)V", false);
        bug1.addSourceLine(new SourceLineAnnotation("TestClass", "TestFile", 69, 6969, 10, 20));
        bug1.addClass("TestClass");
        bugInstances.add(bug1);
        bugInstances.writeXML(firstFile.getAbsolutePath());

        SortedBugCollection secondBugInstance = new SortedBugCollection();
        BugInstance bug2 = new BugInstance("Test2", 1);
        bug2.addMethod("TestClass2", "TestMethod2", "(Lorg/test/TestClass;Ljava/util/List;)V", false);
        bug2.addSourceLine(new SourceLineAnnotation("TestClass2", "TestFile2", 69, 6969, 10, 20));
        bug2.addClass("TestClass2");
        secondBugInstance.add(bug2);
        secondBugInstance.writeXML(secondFile.getAbsolutePath());
        return fileName;
    }
}
