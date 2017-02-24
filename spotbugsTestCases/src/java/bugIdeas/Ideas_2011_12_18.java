package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Ideas_2011_12_18 {
    
    @SuppressFBWarnings("IL_INFINITE_RECURSIVE_LOOP")
    @NoWarning("IL_INFINITE_RECURSIVE_LOOP")
    int loop() {
        return loop();
    }
    
    @ExpectWarning("IL_INFINITE_RECURSIVE_LOOP")
    int loop2() {
        return loop2();
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("IL_INFINITE_RECURSIVE_LOOP")
    @NoWarning("IL_INFINITE_RECURSIVE_LOOP")
    int loop3() {
        return loop3();
    }
}
