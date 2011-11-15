package sfBugs;

import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;


public class Bug3295061<T extends Bug3295061.FooBar &  Comparable<T> & Cloneable & Serializable, S extends Bug3295061.FooBar &  Comparable<S>> implements Serializable {
    
    interface FooBar {}

    private static final long serialVersionUID = 1L;
    @NoWarning("SE_BAD_FIELD")
    T foo;
    
    @DesireWarning("SE_BAD_FIELD")
    S bar;
    
    public void setFoo(T foo) {
        this.foo = foo;
    }
    public void setBar(S bar) {
        this.bar = bar;
    }
}


