package constructorthrow;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * In this test case, the constructor throws a FileNotFoundException,
 * and handles by catching the parent of FileNotFoundException, IOException.
 */
public class ConstructorThrowNegativeTest6 {
    public ConstructorThrowNegativeTest6() {
        try {
            throw new FileNotFoundException();
        } catch (IOException e) {
            System.err.println("IOException caught");
        }
    }
}
