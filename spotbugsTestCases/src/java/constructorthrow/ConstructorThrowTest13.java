package constructorthrow;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;

/**
 * The constructor throws three exceptions (two checked, one unchecked),
 * and two of them are handled in one catch as separate clauses.
 */
public class ConstructorThrowTest13 {
    public ConstructorThrowTest13(File f) {
        try {
            if (!f.exists()) {
                throw new FileNotFoundException();
            } else if (f.canRead()) {
                throw new AccessDeniedException(f.getAbsolutePath());
            } else {
                throw new RuntimeException();
            }
        } catch (FileNotFoundException | AccessDeniedException e) {
            System.err.println("IOException caught");
        }
    }
}
