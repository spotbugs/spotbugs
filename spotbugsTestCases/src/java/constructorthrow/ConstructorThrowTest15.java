package constructorthrow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A simple testcase with a try-with-resources, where the exception is thrown from the catch block.
 */
public class ConstructorThrowTest15 {
    public ConstructorThrowTest15(File f) {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("string");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
