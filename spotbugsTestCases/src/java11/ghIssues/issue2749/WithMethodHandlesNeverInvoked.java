package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created, but neither is invoked
 */
public class WithMethodHandlesNeverInvoked {
    public final class Value {
        // nothing else
    }

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findGetter(WithMethodHandlesNeverInvoked.class, "reflectiveField", Value.class);
            SETTER = lookup.findSetter(WithMethodHandlesNeverInvoked.class, "reflectiveField", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value reflectiveField;
}