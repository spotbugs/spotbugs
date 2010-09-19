package tigerTraps;
public class Parsing {
   /**
    * Returns Integer corresponding to s, or null if s is null.
    * @throws NumberFormatException if s is nonnull and
    *         doesn't represent a valid integer.
    */
   public static Integer parseInt(String s) {
       return (s == null) ?
               (Integer) null : Integer.parseInt(s);
   }

   public static void main(String[] args) {
       System.out.println(parseInt("-1") + " " +
                          parseInt(null) + " " +
                          parseInt("1"));
   }
}
