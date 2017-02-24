package sfBugs;

import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3593735 {
    public static abstract class BadClassExample {
        @ExpectWarning("UUF_UNUSED_FIELD")
        private String unusedField;

        public abstract void doFoo();
    }

    /**
     * This class is serializable; thus, the field might be present for
     * backwards compatibility with serialized version of the class.
     * 
     */
    public static abstract class OkExample implements Serializable {

        private static final long serialVersionUID = 2L;

        @NoWarning("UUF_UNUSED_FIELD")
        private String unusedField;

        public abstract void doFoo();
    }

}
