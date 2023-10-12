package bugIdeas;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Ideas_2010_01_22 {

    public int needsNonnull(Object x) {
        return x.hashCode();
    }

    public int needsNonnull2(@Nonnull Object x) {
        return 17;
    }

    @Test
    public void testNeedsNonnull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            needsNonnull(null);
        });
    }

    @Test
    public void testNeedsNonnull2() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            needsNonnull2(null);
        });
    }

    @Test
    public void testNeedsNonnull3() {
        needsNonnull(null);
    }

    @Test
    public void testNeedsNonnull4() {
        needsNonnull2(null);
    }

    public int badCode() {
        return needsNonnull(null);
    }

    public int badCode2() {
        return needsNonnull2(null);
    }
}
