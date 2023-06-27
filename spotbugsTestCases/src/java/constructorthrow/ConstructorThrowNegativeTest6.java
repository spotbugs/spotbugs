package constructorthrow;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * In this test case, the constructor calls a method,
 * which calls a method, which throws an unchecked exception.
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
