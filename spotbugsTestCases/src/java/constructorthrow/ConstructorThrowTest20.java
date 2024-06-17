package constructorthrow;

import java.io.FileWriter;
import java.io.IOException;

/**
 * The constructor throws an Exception conditionally.
 */
public class ConstructorThrowTest20 {
    public ConstructorThrowTest20() throws IOException {
        if (!verify()) {
            FileWriter fw = new FileWriter("");
            fw.write("string");
        }
    }

    private boolean verify() {
        return false;
    }
}
