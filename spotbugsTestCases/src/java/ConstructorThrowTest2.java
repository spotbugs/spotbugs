import java.io.IOException;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * In this testcase the constructor throws a checked exception,
 * there is a throws declaration. 
 */

public class ConstructorThrowTest2{
    @ExpectWarning("CT")
    public ConstructorThrowTest2() throws IOException {
        throw new IOException(); // Error, constructor throw.
    }
}
