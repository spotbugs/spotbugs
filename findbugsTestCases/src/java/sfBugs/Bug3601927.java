package sfBugs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3601927 {

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public static void transfer(InputStream src, File dest) throws IOException {
        ReadableByteChannel in = Channels.newChannel(src);
        try {
            WritableByteChannel out = new FileOutputStream(dest).getChannel();
            try {
                nioCopy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static void nioCopy(ReadableByteChannel in, WritableByteChannel out) {
        // TODO Auto-generated method stub

    }

}
