package sfBugs;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Bug3160693 {
    public static void copy(File srcFile, File dstFile) throws IOException {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        try {
            // Create channel on the source
            srcChannel = new FileInputStream(srcFile).getChannel();

            // Create channel on the destination
            dstChannel = new FileOutputStream(dstFile).getChannel();

            // Copy file contents from source to destination
            dstChannel.transferFrom(srcChannel,
                    0,
                    srcChannel.size());
        } finally {
            //Note only closing src channel, comment this next line of code and run FindBugs it will detect that here both srcChannel
            //and dstChannel may failed to close. But enabling this next call will result in no longer detecting that this has
            //failed for dstChannel
            safeClose(srcChannel);
        }
    }

    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
            }
        }
    }
}
