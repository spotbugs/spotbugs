package gcUnrelatedTypes;

import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class CollectionsShouldNotContainThemselves {

    @ExpectWarning("DMI")
    public static void main(String args[]) {

        Set s = new HashSet();

        s.contains(s);
        s.remove(s);
        s.containsAll(s);
        s.retainAll(s);
        s.removeAll(s);
    }

}
