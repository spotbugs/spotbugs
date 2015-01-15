package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature278 {
    public enum Test {
        A, B, C;
        
        @ExpectWarning("ME_MUTABLE_ENUM_FIELD")
        public int f;
        
        @NoWarning("ME_MUTABLE_ENUM_FIELD")
        public final int ff = 1;
        
        @NoWarning("ME_MUTABLE_ENUM_FIELD")
        private int q = 2;
        
        @NoWarning("ME_MUTABLE_ENUM_FIELD")
        private int lazy = 0;
        
        @ExpectWarning("ME_ENUM_FIELD_SETTER")
        public void setQ(int q) {
            this.q = q;
        }
        
        @NoWarning("ME_ENUM_FIELD_SETTER")
        public int getLazy(int q) {
            if(lazy == 0) {
                lazy = hashCode();
            }
            return lazy;
        }
    }
}
