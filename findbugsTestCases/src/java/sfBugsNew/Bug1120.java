package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1120 {

    Object globalError;

    Object getGlobalError() {
        return globalError;
    }
    void myMethod() {
      // some code

      if (Math.random() > 0.5)
          globalError = "x";
      

      // some code
    }

    @NoWarning("RCN")
    void myProg() {
      globalError = null;
      myMethod();

      // FindBugs considers this check resundant
      if (globalError != null) {
        // do something

      }
    }
    
}
