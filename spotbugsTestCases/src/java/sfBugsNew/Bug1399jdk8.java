package sfBugsNew;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

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
