package bugIdeas;

import javax.annotation.Nonnull;

public class Ideas_2011_09_20 {
    Object foo;
    
    void setFoo(@Nonnull Object foo) {
        this.foo = foo;
    }
    
    void bar(boolean b) {
        Object x = null;
        if (b) x = "abc";
        setFoo(x);
                
    }

}
