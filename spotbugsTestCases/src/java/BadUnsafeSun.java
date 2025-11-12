import java.lang.reflect.Field;
import sun.misc.Unsafe;

class BadUnsafeSun {

  void method() throws Exception {
    final Field f = Unsafe.class.getDeclaredField("theUnsafe");
    f.setAccessible(true);
    final Unsafe unsafe = (Unsafe) f.get(null);
    unsafe.putAddress(0, 0);
  }

}
