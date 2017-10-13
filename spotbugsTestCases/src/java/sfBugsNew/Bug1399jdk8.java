package sfBugsNew;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1399jdk8 {

    @ExpectWarning("OS_OPEN_STREAM")
    void test1(File f, byte b) throws IOException {
        BufferedReader c = Files.newBufferedReader(Paths.get(""));
        c.hashCode();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test2(File f, byte b) throws IOException {
        BufferedWriter c = Files.newBufferedWriter(Paths.get(""));
        c.hashCode();
    }
}
