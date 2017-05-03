package sfBugsNew.Bug1207b;

import sfBugsNew.Bug1207a.Parent;
import edu.umd.cs.findbugs.annotations.NoWarning;

@NoWarning("USM_USELESS_SUBCLASS_METHOD")
public class Child extends Parent {
    @Override
    protected void protectedMethod() {
        super.protectedMethod();
    }
}