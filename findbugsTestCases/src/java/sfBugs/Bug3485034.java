package sfBugs;

import java.io.IOException;
import java.io.InputStream;

public class Bug3485034 {
    public void go() throws IOException {
        InputStream stream = Bug3485034.class.getResourceAsStream("");
        System.out.println(stream.read());
    }
}
