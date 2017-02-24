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
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1399 {

    @ExpectWarning("OS_OPEN_STREAM")
    void test1(File f, byte b) throws IOException {
        InputStream stream = Files.newInputStream(Paths.get(""));
        stream.read();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test2(File f, byte b) throws IOException {
        OutputStream stream = Files.newOutputStream(Paths.get(""));
        stream.write(0);
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test3(File f, byte b) throws IOException {
        SeekableByteChannel c = Files.newByteChannel(Paths.get(""));
        c.position();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test4(File f, byte b) throws IOException {
        SeekableByteChannel c = Files.newByteChannel(Paths.get(""), StandardOpenOption.APPEND);
        c.position();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test5(File f, byte b) throws IOException {
        DirectoryStream<Path> c = Files.newDirectoryStream(Paths.get(""));
        c.hashCode();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test6(File f, byte b) throws IOException {
        DirectoryStream<Path> c = Files.newDirectoryStream(Paths.get(""), (Filter)null);
        c.hashCode();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test7(File f, byte b) throws IOException {
        DirectoryStream<Path> c = Files.newDirectoryStream(Paths.get(""), "");
        c.hashCode();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test8(File f, byte b) throws IOException {
        BufferedReader c = Files.newBufferedReader(Paths.get(""), Charset.defaultCharset());
        c.hashCode();
    }

    @ExpectWarning("OS_OPEN_STREAM")
    void test9(File f, byte b) throws IOException {
        BufferedWriter c = Files.newBufferedWriter(Paths.get(""), Charset.defaultCharset());
        c.hashCode();
    }
}
