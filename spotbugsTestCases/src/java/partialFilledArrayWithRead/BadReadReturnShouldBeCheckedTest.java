package partialFilledArrayWithRead;


import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BadReadReturnShouldBeCheckedTest {

    String readBytes(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        if (in.read(data) == -1) {
            throw new EOFException();
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    public static String readBytesWithOffset(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        int offset = 0;
        if (in.read(data, offset, data.length - offset) != -1) {
            throw new EOFException();
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    public String readFromBufferedReader(BufferedReader bufferedReader) throws IOException {
        char[] data = new char[1024];

        if (bufferedReader.read(data) == -1) {
            throw new EOFException();
        }
        return new String(data);
    }

    String readBytesComparedToFloat(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        if (in.read(data) == -1F) {
            throw new EOFException();
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    String changeOrderOfComparing(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        if (-1 == in.read(data)) {
            throw new EOFException();
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    String changeOrderOfComparingLong(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        if (-1L == in.read(data)) {
            throw new EOFException();
        }
        return new String(data, StandardCharsets.UTF_8);
    }
}
