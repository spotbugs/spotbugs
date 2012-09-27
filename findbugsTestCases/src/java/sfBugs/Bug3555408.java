package sfBugs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotation(NonNull.class)
public class Bug3555408 {
    
@NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public static final String constant = "constant";

@NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public static final Integer constantInt = Integer.valueOf(5);

private boolean x;

@NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public Bug3555408() {}

public boolean isX() {
return x;
}

public void setX(boolean x) {
this.x = x;
}

}
