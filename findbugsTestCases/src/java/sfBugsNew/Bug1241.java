package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1241 {

    @DesireNoWarning("NP")
    static int nullSafeStringComparator(final String one, final String two) {
        if (one == null ^ two == null) {
            return (one == null) ? -1 : 1;
        }

        if (one == null && two == null) {
            return 0;
        }

        return one.compareTo(two); // Bug found on this line
    }

    @NoWarning("NP")
    static int nullSafeStringComparatorFixed(final String one, final String two) {
        if (one == null ^ two == null) {
            return (one == null) ? -1 : 1;
        }

        if (one == null) {
            return 0;
        }

        return one.compareTo(two); // Bug found on this line
    }
    @NoWarning("NP")
    static int nullSafeStringComparatorFixed2(final String one, final String two) {
        if (one == null ^ two == null) {
            return (one == null) ? -1 : 1;
        }

        if (two == null) {
            return 0;
        }

        return one.compareTo(two); // Bug found on this line
    }
    
    @NoWarning("NP")
    static int nullSafeStringComparatorFixed3(final String one, final String two) {
        if (one == null ^ two == null) {
            return (one == null) ? -1 : 1;
        }

        if (one == two) {
            return 0;
        }

        return one.compareTo(two); // Bug found on this line
    }

}
