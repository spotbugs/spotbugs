package sfBugs;

import java.io.IOException;

public class Bug2382440 {
    void foo(int x) {
        try {
            someCallthatMightThrowException(x);
        } catch (Exception e) {
            // Ignore, use default.
        }
    }

    private String someCallthatMightThrowException(int x) throws IOException {
        if (x == 42)
            throw new IOException();
        return "Yes";
    }

}
