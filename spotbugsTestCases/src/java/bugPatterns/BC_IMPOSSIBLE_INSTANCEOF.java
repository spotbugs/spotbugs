package bugPatterns;

import jakarta.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class BC_IMPOSSIBLE_INSTANCEOF {
    // seen in edu.umd.cs.findbugs.ba.IncompatibleTypes

    @ExpectWarning("BC_IMPOSSIBLE_INSTANCEOF")
    public static @Nonnull
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

class Type {

}

class BasicType extends Type {

}

class ReferenceType extends Type {

}
