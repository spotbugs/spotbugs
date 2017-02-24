package sfBugs;

import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3599765 {

    
    private final MultiValueMap<String, String> otherParams = new LinkedMultiValueMap<String, String>();

    @NoWarning("BC")
    public void setOtherParam(String name, List<String> values) {
        List<String> otherP = otherParams.get(name); // triggers bug: Impossible
                                                     // cast from String to
                                                     // java.util.List
        if (otherP == null) {
            otherParams.put(name, values);
        } else {
            otherP.addAll(values);
        }
    }

}
