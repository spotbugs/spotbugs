package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1240<T extends Data & Displayable> {
    T field;

    @DesireNoWarning("BC_UNCONFIRMED_CAST")
    public String getFromLocalVar(T value) {
        return value.getDisplayKey();
    }

    @DesireNoWarning("BC_UNCONFIRMED_CAST")
    public String getFomField(T value) {
        return field.getDisplayKey();
    }

}

interface Displayable {
    String getDisplayKey();
}

class Data {
}
