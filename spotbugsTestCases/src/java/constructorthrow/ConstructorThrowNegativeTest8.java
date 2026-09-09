package constructorthrow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 * The constructor throws two exceptions, and handles both of them in separate catches.
 */
public class ConstructorThrowNegativeTest8 {
    public ConstructorThrowNegativeTest8(File f) {
        try {
            if (!f.exists()) {
                throw new FileNotFoundException();
            } else {
                throw new UnsupportedEncodingException();
            }
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException caught");
        } catch (UnsupportedEncodingException e) {
            System.err.println("UnsupportedEncodingException caught");
        }
    }
}
