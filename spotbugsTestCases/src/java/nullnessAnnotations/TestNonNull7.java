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
    public void objectsrequireNonNull1(File rootFile) {
        for (File subFile : Objects.requireNonNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    public void objectsNonNull1(File rootFile) {
        File[] files = rootFile.listFiles();
        if (Objects.nonNull(files)) {
            for (File subFile : files) {
                System.out.println(subFile.getName());
            }
        }
    }

    public void verifyverifyNotNull1(File rootFile) {
        for (File subFile : Verify.verifyNotNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    public void validatenotNull1(File rootFile) {
        for (File subFile : Validate.notNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    public void preconditionsNotNull(File rootFile) {
        for (File subFile : Preconditions.checkNotNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

    public void nocheck(File rootFile) {
        for (File subFile : rootFile.listFiles()) {
            System.out.println(subFile.getName());
        }
    }
}
