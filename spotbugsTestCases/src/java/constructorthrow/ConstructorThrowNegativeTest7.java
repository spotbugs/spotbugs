package constructorthrow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 * The constructor throws two exceptions, and handles both of them in one catch.
 */
public class ConstructorThrowNegativeTest7 {
    public ConstructorThrowNegativeTest7(File f) {
        try {
            if (!f.exists()) {
                throw new FileNotFoundException();
            } else {
                throw new UnsupportedEncodingException();
            }
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            System.err.println("IOException caught");
        }
    }
}
