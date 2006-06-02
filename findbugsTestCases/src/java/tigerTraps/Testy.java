package tigerTraps;
import java.lang.reflect.*;
@interface Test { }
public class Testy {
   @Test public static void test() { System.out.print("Pass"); }
   @Test public static void test2() { throw null; }
   public static void main(String[] args) throws Exception {
       for (Method m : Testy.class.getDeclaredMethods()) {
           if (m.isAnnotationPresent(Test.class)) {
               try {
                   m.invoke(null);
               } catch (Throwable ex) {
                   System.out.println("Fail");
               }
           }
       }
   }
}