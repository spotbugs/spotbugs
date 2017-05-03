package sfBugs;

import net.jcip.annotations.Immutable;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;

@Immutable
public class Bug3303905 {
    private Bug3303905(String sourceName, int lineNumber) {
        super();
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
    }

    private final String sourceName;

    private final int lineNumber;

    @DesireNoWarning("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
    private transient int hashCode;

    @DesireNoWarning("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
    private transient String toString;

    public int hashCode() {
        if (hashCode == 0)
            hashCode = sourceName.hashCode() + lineNumber;
        return hashCode;
    }

    public String toString() {
        if (toString == null)
            toString = sourceName + ":" + lineNumber;
        return toString;
    }
}