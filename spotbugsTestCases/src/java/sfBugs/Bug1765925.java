package sfBugs;

import java.io.File;

/*
 * Submitted By: Alfred Nathaniel
 * Summary:
 *
 * Findbugs should ignore PZLA_PREFER_ZERO_LENGTH_ARRAYS in all methods named
 * "listFiles" since returning null is the convention established by
 * java.io.File.
 */
public class Bug1765925 {
    private File file;

    Bug1765925(File file) {
        this.file = file;
    }

    // // grep -A 1 PZLA_PREFER_ZERO_LENGTH_ARRAYS | grep Bug1765925
    File[] listFiles() {
        File[] files = file.listFiles();
        if (files == null)
            return null;
        else {
            return new File[] {};
        }
    }
}
