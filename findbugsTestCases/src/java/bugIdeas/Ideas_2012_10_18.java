package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2012_10_18 {
    
    final static int DEFAULT_PORT = 80;
    final static Integer DEFAULT_BOXED_PORT = 80;
    
    Integer port;
    int p;
    
    @ExpectWarning("BX_UNBOXING_IMMEDIATELY_REBOXED")
    public void warning( Integer port) {
        this.port = port == null ? DEFAULT_PORT : port;
    }
    
    @NoWarning("BX_UNBOXING_IMMEDIATELY_REBOXED")
    public void OK( Integer port) {
        this.p = port == null ? DEFAULT_PORT : port;
        this.port = port == null ? DEFAULT_BOXED_PORT : port;
        this.p = port == null ? DEFAULT_BOXED_PORT : port;
    }

}
