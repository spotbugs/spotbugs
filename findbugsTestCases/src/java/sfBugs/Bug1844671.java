package sfBugs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1844671 {
    @NoWarning("OS_OPEN_STREAM")
    public void falsePositive1() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(""));
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                if (fw != null) { // no false positive
                    fw.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    @NoWarning("OS_OPEN_STREAM")
    public void falsePositive2() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(""));
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                if (null != fw) { // false positive
                    fw.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

   public void complicated() {
        FileWriter tmp = null;
        FileWriter fw = null;
        try {
            tmp = new FileWriter(new File("")); 
            fw = new FileWriter(new File("")); 
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                if (tmp != fw) {
                    if (fw != null)
                        fw.close();
                    tmp.close();
                }
            } catch (IOException ioe) {
            }
        }
    }
}
