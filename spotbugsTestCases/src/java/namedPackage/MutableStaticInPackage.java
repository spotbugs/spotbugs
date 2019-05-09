package namedPackage;

import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class MutableStaticInPackage {
    @ExpectWarning("MS_MUTABLE_COLLECTION")
    public static final List<String> LIST = Arrays.asList("a", "b");

    @NoWarning("MS_MUTABLE_COLLECTION")
    public static final List<String> EMPTY_LIST = Arrays.asList();
}
