package ghIssues;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class Issue4304 {
    // The example from the issue: the wrapped stream is closed, so nothing leaks
    public void closeWrappedPrintStream() throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        PrintStream writer = new PrintStream(stream);
        writer.println("value");
        writer.flush();
        stream.close();
    }

    public void closeWrappedBufferedOutputStream() throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        BufferedOutputStream buffered = new BufferedOutputStream(stream);
        buffered.write(1);
        buffered.flush();
        stream.close();
    }

    public void closeWrappedPrintWriter() throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        PrintWriter writer = new PrintWriter(stream);
        writer.println("value");
        writer.flush();
        stream.close();
    }

    // Two levels of wrapping, closing the innermost stream
    public void closeInnermostOfTwoWrappers() throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        OutputStreamWriter streamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        PrintWriter writer = new PrintWriter(streamWriter);
        writer.println("value");
        writer.flush();
        stream.close();
    }

    public int closeWrappedInputStream() throws IOException {
        FileInputStream stream = new FileInputStream("file.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        int c = reader.read();
        stream.close();
        return c;
    }

    // Closing the wrapper was already handled; it must stay unreported
    public void closeWrapper() throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        PrintStream writer = new PrintStream(stream);
        writer.println("value");
        writer.close();
    }

    // Genuine leak: nothing is closed
    public void closeNothing() throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        PrintStream writer = new PrintStream(stream);
        writer.println("value");
    }

    // Genuine leak: a different, unrelated stream is closed
    public void closeUnrelatedStream() throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        PrintStream writer = new PrintStream(stream);
        writer.println("value");
        FileOutputStream other = new FileOutputStream("other.txt");
        other.close();
    }

    // Genuine leak: the wrapped stream is only closed on one path
    public void closeWrappedOnOnePathOnly(boolean flag) throws IOException {
        FileOutputStream stream = new FileOutputStream("file.txt");
        PrintStream writer = new PrintStream(stream);
        writer.println("value");
        if (flag) {
            stream.close();
        }
    }
}
