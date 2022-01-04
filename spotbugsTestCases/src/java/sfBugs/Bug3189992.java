package sfBugs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Bug3189992 {
    public void method() {
        try {
            new String(new byte[] {}, "xyz");
        } catch (UnsupportedEncodingException e) {
            IOException pe = new IOException("cannot parse URL query part");
            pe.initCause(e);
        }
    }
}
