package sfBugsNew;

import sfBugsNew.Bug1240.Displayable;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1240<T extends Enum<T> & Displayable> {

    public interface Displayable {
        String getDisplayKey();
    }

    @DesireNoWarning("BC_UNCONFIRMED_CAST")
    public String getDisplayKeyForValue(T value) {
        return value == null ? null : value.getDisplayKey();
    }

}
