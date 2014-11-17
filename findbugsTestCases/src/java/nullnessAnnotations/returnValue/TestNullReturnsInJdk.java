package nullnessAnnotations.returnValue;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class TestNullReturnsInJdk {

    private static final Path PATH = Paths.get("");
    private static final File FILE = PATH.toFile();

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe3() throws IOException{
        use(Files.probeContentType(PATH).length());
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe4() throws IOException{
        use(PATH.getFileName().getNameCount());
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe5() throws IOException{
        use(PATH.getParent().getNameCount());
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe6() throws IOException{
        use(PATH.getRoot().getNameCount());
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe7() throws IOException{
        use(FILE.list().length);
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe8() throws IOException{
        use(FILE.list(null).length);
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe9() throws IOException{
        use(FILE.listFiles().length);
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe10() throws IOException{
        use(FILE.listFiles((FileFilter)null).length);
    }

    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    void npe11() throws IOException{
        use(FILE.listFiles((FilenameFilter)null).length);
    }

    private void use(Object o) {
        System.out.println(o);
    }
}
