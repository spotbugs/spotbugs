package bugIdeas;

import com.google.common.base.Strings;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2012_01_13 {
    
    @ExpectWarning("DMI_DOH")
    public boolean testIsNullOrEmpty() {
        return Strings.isNullOrEmpty("uid");
    }

    @ExpectWarning("DMI_DOH")
    public String testEmptyToNull() {
        return Strings.emptyToNull("uid");
    }

    @ExpectWarning("DMI_DOH")
    public String testNullToEmpty() {
        return Strings.nullToEmpty("uid");
    }

}
