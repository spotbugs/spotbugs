import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EmptyEntry {
    public void testZip(ZipOutputStream zos) throws Exception {
        ZipEntry ze = new ZipEntry("foo");
        zos.putNextEntry(ze);
        zos.closeEntry();
    }

    public void testJar(JarOutputStream jos) throws Exception {
        JarEntry je = new JarEntry("foo");
        jos.putNextEntry(je);
        jos.closeEntry();
    }
}
