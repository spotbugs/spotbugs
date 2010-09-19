package bugPatterns;

import java.io.BufferedInputStream;
import java.io.IOException;

public class SR_NOT_CHECKED {

    void bug1(BufferedInputStream any, long anyLong) throws IOException {
        any.skip(anyLong);
    }

    void bug2(BufferedInputStream any, long anyLong) throws IOException {
        any.skip(anyLong);
    }

    long notBug(BufferedInputStream any, long anyLong) throws IOException {
        return any.skip(anyLong);
    }

    void notBug2(BufferedInputStream any, long anyLong) throws IOException {
        while (anyLong > 0) {
            anyLong -= any.skip(anyLong);
        }

    }

}
