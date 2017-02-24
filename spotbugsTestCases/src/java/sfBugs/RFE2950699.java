package sfBugs;

import java.io.File;

public class RFE2950699 {
    @SuppressWarnings({ "DMI_HARDCODED_ABSOLUTE_FILENAME" })
    private final File x = new File("/home/dannyc/workspace/j2ee/src/share/com/sun/enterprise");
}
