package bugPatterns;

import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AM_CREATES_EMPTY_ZIP_FILE_ENTRY {

    void bug(ZipOutputStream any, ZipEntry anyZipEntry) throws IOException {
        any.putNextEntry(anyZipEntry);
        any.closeEntry();
    }

    void bug(JarOutputStream any, ZipEntry anyZipEntry) throws IOException {
        any.putNextEntry(anyZipEntry);
        any.closeEntry();
    }

    void notBug(ZipOutputStream any1, ZipOutputStream any2, ZipEntry anyZipEntry) throws IOException {
        any1.putNextEntry(anyZipEntry);
        any2.closeEntry();
    }

    void notBug(ZipOutputStream any, ZipEntry anyZipEntry, int anyValue) throws IOException {
        any.putNextEntry(anyZipEntry);
        any.write(anyValue);
        any.closeEntry();
    }

    void bug2(ZipOutputStream any, ZipEntry anyZipEntry, int anyValue) throws IOException {
        any.putNextEntry(anyZipEntry);
        any.closeEntry();
        any.write(anyValue);
    }

    void bug3(ZipOutputStream any, ZipEntry anyZipEntry, int anyValue) throws IOException {
        any.write(anyValue);
        any.putNextEntry(anyZipEntry);
        any.closeEntry();
    }

    void notBug(ZipOutputStream any, ZipEntry anyZipEntry, byte[] anyBytes) throws IOException {
        any.putNextEntry(anyZipEntry);
        any.write(anyBytes);
        any.closeEntry();
    }

    void notBug(ZipOutputStream any, ZipEntry anyZipEntry, byte[] anyBytes, int anyOffset, int anySize) throws IOException {
        any.putNextEntry(anyZipEntry);
        any.write(anyBytes, anyOffset, anySize);
        any.closeEntry();
    }

}
