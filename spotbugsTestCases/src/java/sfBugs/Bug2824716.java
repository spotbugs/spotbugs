package sfBugs;

import javax.annotation.CheckReturnValue;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

@DefaultAnnotation({ CheckReturnValue.class })
public class Bug2824716 {

    @CheckReturnValue(when = When.NEVER)
    public Bug2824716 append() {
        return this;
    }

    public Bug2824716 foo() {
        return this;
    }

    public void test() {
        new Bug2824716().append().foo();
    }

}
