package sfBugs;

import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

@DesireNoWarning("SE_BAD_FIELD")
public class Bug3295061<T extends Bug3295061.FooBar &  Comparable<T> & Cloneable & Serializable> implements Serializable {
    
    interface FooBar {}

    private static final long serialVersionUID = 1L;
    T field;
    
    public void setField(T field) {
        this.field = field;
    }
}

