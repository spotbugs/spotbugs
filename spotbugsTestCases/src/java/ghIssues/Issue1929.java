package ghIssues;

import java.io.File;

public class Issue1929 {

    void hardcodedAbsoluteFilenameWithConcat(File vfile) {
        File pipe = new File("/tmp/fswebcam-pipe-" + vfile.getName() + ".mjpeg");
    }
}
