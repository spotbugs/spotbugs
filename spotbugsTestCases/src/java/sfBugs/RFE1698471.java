package sfBugs;

import java.util.LinkedHashSet;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * Based on bug pattern in
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6526376
 * 
 */
public class RFE1698471 {
    private static LinkedHashSet<String> files = new LinkedHashSet<String>();

    @ExpectWarning("NP_SYNC_AND_NULL_CHECK_FIELD")
    static void add(String file) {
        synchronized (files) {
            if (files == null)
                throw new IllegalStateException();

            files.add(file);
        }
    }

    public LinkedHashSet<String> getFiles() {
        LinkedHashSet<String> myFiles;
        synchronized (files) {
            myFiles = files;
            files = null;
        }
        return myFiles;
    }

}
