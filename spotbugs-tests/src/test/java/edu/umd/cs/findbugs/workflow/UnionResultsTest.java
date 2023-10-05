package edu.umd.cs.findbugs.workflow;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UnionResultsTest {
    @Test
    public void testMain() throws IOException {
        //Prepare
        String fileName = createBugFile();
        File outputFile = new File("src/test/resources/output.xml");

        //Act
        UnionResults.main(new String[] { "-withMessages", "-output", outputFile.getAbsolutePath(), fileName });

        //Verify
        List<String> output = readOutPut(outputFile.getAbsolutePath());
        Assert.assertTrue(output.stream().anyMatch(line -> line.contains("(Lorg/test/TestClass;Ljava/util/List;)V")));
        Assert.assertTrue(output.stream().anyMatch(line -> line.contains("(Lorg/test/TestClass2;Ljava/util/List;)V")));

        //Cleanup
        Files.deleteIfExists(new File(fileName).toPath());
        Files.deleteIfExists(outputFile.toPath());
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
        File firstFile = new File("src/test/resources/firstFile.xml");
        File secondFile = new File("src/test/resources/secondFile.xml");
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
