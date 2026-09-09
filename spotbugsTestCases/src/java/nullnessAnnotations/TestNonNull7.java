package nullnessAnnotations;

import com.google.common.base.Verify;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.util.Objects;

public class TestNonNull7 {
    /**
     * Issue <a href="https://github.com/spotbugs/spotbugs/issues/456">#456</a>.
     */
    public void objectsrequireNonNull(File rootFile) {
        for (File subFile : Objects.requireNonNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    public void objectsrequireNonNullFalse(File rootFile) {
        for (File subFile : requireNonNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    private <T> T requireNonNull(T obj) {
        return obj;
    }

    public void objectsNonNull(File rootFile) {
        File[] files = rootFile.listFiles();
        if (Objects.nonNull(files)) {
            for (File subFile : files) {
                System.out.println(subFile.getName());
            }
        }
    }

    public void verifyverifyNotNull(File rootFile) {
        for (File subFile : Verify.verifyNotNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    public void validatenotNull(File rootFile) {
        for (File subFile : Validate.notNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    public void preconditionsNotNull(File rootFile) {
        for (File subFile : Preconditions.checkNotNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }
}
