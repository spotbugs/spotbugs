package constructorthrow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A testcase with a try-catch with finally, having an inner try-catch inside finally.
 * The exception is thrown from the catch of the inner try-catch inside finally.
 */
public class ConstructorThrowTest14 {
    public ConstructorThrowTest14(File f) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            fw.write("string");
        } catch (IOException e) {
            System.err.println("Exception caught, no error");
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
