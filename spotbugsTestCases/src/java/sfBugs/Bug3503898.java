package sfBugs;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3503898 {

    @NoWarning("UrF")
    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    public boolean test(List<String> list) {
        for (String s : list) {
            if (s.equals(this.value)) {
                return true;
            }
        }
        return false;
    }

}
