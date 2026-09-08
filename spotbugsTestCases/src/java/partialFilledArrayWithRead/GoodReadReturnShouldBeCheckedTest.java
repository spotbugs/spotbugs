package partialFilledArrayWithRead;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

public class GoodReadReturnShouldBeCheckedTest {
    public static String readBytes(InputStream in) throws IOException {
        int offset = 0;
        int bytesRead = 0;
        byte[] data = new byte[1024];
        while ((bytesRead = in.read(data, offset, data.length - offset)) != -1) {
            offset += bytesRead;
            if (offset >= data.length) {
                break;
            }
        }
        return new String(data, 0, offset, StandardCharsets.UTF_8);
    }

    public static String singleReadWithProperReturnCheck(FileInputStream fis) throws IOException {
        int size = 1024;
        byte[] data = new byte[size];
        DataInputStream dis = new DataInputStream(fis);
        if (dis.read(data) < size) {
            throw new IOException(String.format("Failed to read %s bytes", size));
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    public static String readFullyBytes(FileInputStream fis) throws IOException {
        byte[] data = new byte[1024];
        DataInputStream dis = new DataInputStream(fis);
        dis.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static String readBytesWithDoWhile(InputStream in) throws IOException {
        int offset = 0;
        int bytesRead;
        byte[] data = new byte[1024];
        do {
            bytesRead = in.read(data, offset, data.length - offset);
            if (bytesRead == -1) {
                break;
            }
            offset += bytesRead;
        } while (offset < data.length);
        return new String(data, 0, offset, StandardCharsets.UTF_8);
    }

    public static String readBytesWithFor(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        int offset = 0;

        for (int bytesRead; offset < data.length; offset += bytesRead) {
            bytesRead = in.read(data, offset, data.length - offset);
            if (bytesRead == -1) {
                break;
            }
        }
        return new String(data, 0, offset, StandardCharsets.UTF_8);
    }

    public static String properReadCheckUsingReaderClass(BufferedReader reader) throws IOException {
        char[] buffer = new char[1024];
        int offset = 0;
        int charsRead;

        while (offset < buffer.length
                && (charsRead = reader.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += charsRead;
        }

        return new String(buffer, 0, offset);
    }

    public static String checkReadWithNonInt(FileInputStream in) throws IOException {
        long size = 1024L;
        byte[] data = new byte[(int) size];
        DataInputStream dis = new DataInputStream(in);
        if (dis.read(data) < size) {
            throw new IOException(String.format("Failed to read %s bytes", size));
        }
        return new String(data, StandardCharsets.UTF_8);
    }
}
