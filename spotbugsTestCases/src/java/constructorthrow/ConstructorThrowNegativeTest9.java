package constructorthrow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A simple testcase with a try-catch, where the exception is handled.
 */
public class ConstructorThrowNegativeTest9 {
    public ConstructorThrowNegativeTest9(File f) {
        try {
            FileWriter fw = new FileWriter(f);
            fw.write("string");
        } catch (IOException e) {
            System.err.println("Exception caught, no error");
        }
    }
}
