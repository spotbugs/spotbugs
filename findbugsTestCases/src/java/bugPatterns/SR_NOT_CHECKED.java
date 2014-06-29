package bugPatterns;

import java.io.BufferedInputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class SR_NOT_CHECKED {

    @ExpectWarning("SR_NOT_CHECKED")
    void bug1(BufferedInputStream any, long anyLong) throws IOException {
        any.skip(anyLong);
    }

    @ExpectWarning("SR_NOT_CHECKED")
    void bug2(BufferedInputStream any, long anyLong) throws IOException {
        any.skip(anyLong);
    }

    @NoWarning("SR_NOT_CHECKED")
    long notBug(BufferedInputStream any, long anyLong) throws IOException {
        return any.skip(anyLong);
    }

    @NoWarning("SR_NOT_CHECKED")
    void notBug2(BufferedInputStream any, long anyLong) throws IOException {
        while (anyLong > 0) {
            anyLong -= any.skip(anyLong);
        }

    }

}
