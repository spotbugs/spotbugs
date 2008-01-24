public class Foo {
    public static void main(String args[]) {
        int[] mya = new int[6];
        System.out.println(mya.toString());
        
        String s = "Hello";
        s.toUpperCase();
        System.out.println(s);
        
        for(int i = 0; i < 1000; i++) {
        	s += "Hello";
        }
        s.toLowerCase();
        System.out.println(s);
        
        Integer i = new Integer(0);
        System.out.println("" + i);
    }
}
