package nullnessAnnotations;

import java.io.File;
import java.util.Objects;

public class TestNonNull7 {
    /**
     * Issue <a href="https://github.com/spotbugs/spotbugs/issues/456">#456</a>.
     */
    public void ObjectsrequireNonNull1(String[] args) {
        File rootFile = new File("some_unexisting_file");
        for (File subFile : Objects.requireNonNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

}
