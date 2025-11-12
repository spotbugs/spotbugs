import java.lang.reflect.Field;

class BadUnsafeSunQualified {

  void method() throws Exception {
    final Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    f.setAccessible(true);
    final sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
    unsafe.putAddress(0, 0);
  }

}
