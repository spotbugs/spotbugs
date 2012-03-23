package sfBugs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3509046 implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    static class Cell implements Serializable {
        private static final long serialVersionUID = 7526472295622776147L;
    }
    
    @NoWarning("SE_BAD_FIELD")
    private final Map<String, Cell> map = new HashMap<String, Cell>();

    @NoWarning("SE_BAD_FIELD")
    private final Map<String, String> aliases = new HashMap<String, String>();

    @NoWarning("SE_BAD_FIELD")
    private final Map<String, String> columns = new HashMap<String, String>();

    
    @NoWarning("SE_BAD_FIELD")
    private  Map<String, String> foo;

}
