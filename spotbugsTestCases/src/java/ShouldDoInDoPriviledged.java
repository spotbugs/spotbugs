import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

public class ShouldDoInDoPriviledged implements Cloneable {

    static AtomicInteger id = new AtomicInteger();

    final int x = id.getAndIncrement();

    @Override
    public ShouldDoInDoPriviledged clone() throws CloneNotSupportedException {
        try {
            ShouldDoInDoPriviledged c = (ShouldDoInDoPriviledged) super.clone();
            Field xField = ShouldDoInDoPriviledged.class.getField("x");
            xField.setAccessible(true);
            xField.setInt(c, id.getAndIncrement());
            return c;
        } catch (Exception e) {
            throw new CloneNotSupportedException();
        }
    }

}
