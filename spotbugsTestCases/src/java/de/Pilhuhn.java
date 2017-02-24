package de;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public abstract class Pilhuhn {

    public abstract void somethingDangerous() throws Exception;

    /** bug 1441505, but i am unable to reproduce */
    void closem(FileInputStream fis, FileOutputStream fos) {
        try {
            somethingDangerous();
        } catch (Exception exc) {

            if (fis != null)
                try {
                    fis.close();
                } catch (Exception e2) {
                }

            if (fos != null)
                try {
                    fos.close();
                } catch (Exception e2) {
                } // DE_MIGHT_IGNORE

            throw new RuntimeException(exc);
        }
    }

}
