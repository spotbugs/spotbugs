
public class FormatString {
    public static void main(String args[]) {
        java.util.Formatter f = new java.util.Formatter();
        System.out.printf("%d", 7);
        System.out.format("%d", 8);
        System.out.println(String.format("%d %s", 8, "boy"));
        System.out.println(f.format("%d %s %f %d %f %d", 8, "boy", 7.7, 9, 9.9, 10));
        System.out.println(f.format("%d %s %f %d %f", 8, "boy", 7.7, 9, 9.9, 10));
        System.out.println(f.format("%d %s %f %d %f %d", 8, "boy", 7.7, 9, 9.9, 10, 11));
        System.out.println(f.format("%d %%%%%s %% %f %d %%%f %%%d", 8, "boy", 7.7, 9, 9.9, 10));
        varargsMethod((Object[])args);
    }
    
    private static void varargsMethod(Object... args) {
        if(args[0] instanceof String) {
            System.out.println((String)args[0]);
        }
    }
}
