package bugIdeas;

import org.jspecify.annotations.NonNull;

public class Ideas_2011_09_20 {
    Object foo;

    void setFoo(@NonNull Object foo) {
        this.foo = foo;
    }

    void bar(boolean b) {
        Object x = null;
        if (b) x = "abc";
        setFoo(x);

    }

}
