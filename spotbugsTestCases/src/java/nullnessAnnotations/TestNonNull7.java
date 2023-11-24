package nullnessAnnotations;

import java.io.File;
import java.util.Objects;


public class TestNonNull7 {

    public static void main(String[] args) {
        File rootFile = new File("some_unexisting_file");
        for (File subFile : Objects.requireNonNull(rootFile.listFiles())) {
            System.out.println(subFile.getName());
        }
    }

}
