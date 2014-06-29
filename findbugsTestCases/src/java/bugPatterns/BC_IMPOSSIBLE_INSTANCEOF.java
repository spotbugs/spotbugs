package bugPatterns;

import javax.annotation.Nonnull;

import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;

public class BC_IMPOSSIBLE_INSTANCEOF {
    // seen in edu.umd.cs.findbugs.ba.IncompatibleTypes

    @ExpectWarning(value = "BC_IMPOSSIBLE_INSTANCEOF", num = 1)
    static public @Nonnull
    boolean getPriorityForAssumingCompatible(Type expectedType, Type actualType, boolean pointerEquality) {
        if (expectedType.equals(actualType))
            return true;

        if (!(expectedType instanceof ReferenceType))
            return true;
        if (!(actualType instanceof ReferenceType))
            return true;

        if (expectedType instanceof BasicType ^ actualType instanceof BasicType) {
            return false;
        }
        return false;
    }
}
