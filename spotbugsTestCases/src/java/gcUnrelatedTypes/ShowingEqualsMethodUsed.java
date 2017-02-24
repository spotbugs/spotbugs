package gcUnrelatedTypes;

import java.util.HashSet;
import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class ShowingEqualsMethodUsed {

    HashSet<String> set = new HashSet<String>();

    @ExpectWarning("GC")
    public boolean testByteArray(byte[] b) {
        return set.contains(b);
    }

    @ExpectWarning("GC")
    public boolean testList(List<String> lst) {
        return set.contains(lst);
    }

}
