package constructorthrow;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The constructor throws two exceptions, but only catches one of them.
 */
public class ConstructorThrowTest12 {
    public ConstructorThrowTest12(File f) {
        try {
            if (!f.exists()) {
                throw new FileNotFoundException();
            } else {
                throw new RuntimeException();
            }
        } catch (FileNotFoundException e) {
            System.err.println("IOException caught");
        }
    }
}
