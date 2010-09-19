package sfBugs;

import javax.annotation.CheckReturnValue;
import javax.annotation.meta.When;

@CheckReturnValue
public class Bug2824716a {

    @CheckReturnValue(when = When.NEVER)
    public Bug2824716a append() {
        return this;
    }

    public Bug2824716a foo() {
        return this;
    }

    public void test() {
        new Bug2824716a().append().foo();
    }
}
