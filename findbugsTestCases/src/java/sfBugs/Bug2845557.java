package sfBugs;

import java.io.File;
import java.io.IOException;

public class Bug2845557 {

    public Object iAmCreatingAnObject() {
        return new Object() {
            private byte[] iHaveToThrowAnException() throws IOException {
                return Bug2845557.this.iThrowAnException();
            }
        };
    }

    private byte[] iThrowAnException() throws IOException {
        File.createTempFile("foo", "bar");
        return "Test".getBytes("UTF-8");
    }
}
