package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1964520a {

    private Superclass something;

    @NoWarning("BC_UNCONFIRMED_CAST")
    public void setSomething(Superclass object) {
        this.something = object;
        if (something instanceof Subclass && ((Subclass) something).bla()) 
        {
            ((Subclass) something).foo();
        }
    }

    private static class Superclass {
        //
    }

    private static class Subclass extends Superclass {
        public boolean bla() {
            return true;
        }

        public void foo() {
            //
        }
    }

}
