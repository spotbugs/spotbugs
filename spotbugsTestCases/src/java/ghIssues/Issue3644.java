package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

abstract class Issue3644AbstractMethod {
    @NoWarning("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION")
    public abstract void abstractMethod() throws Exception;

    @ExpectWarning("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION")
    public void concreteMethod() throws Exception {
        throw new Exception();
    }
}

interface Issue3644InterfaceMethod {
    @NoWarning("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION")
    void interfaceMethod() throws Exception;
}
