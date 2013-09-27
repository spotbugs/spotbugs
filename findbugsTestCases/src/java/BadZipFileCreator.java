import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class BadZipFileCreator {

    
    @ExpectWarning("AM_CREATES_EMPTY_ZIP_FILE_ENTRY")
    public static void main(String args[]) throws Exception {
        ZipOutputStream zipfile = new ZipOutputStream(new FileOutputStream("foo.zip"));
        for (int i = 0; i < args.length; i++) {
            ZipEntry e = new ZipEntry(args[i]);
            zipfile.putNextEntry(e);
            zipfile.closeEntry();
        }
        zipfile.close();
    }
}
