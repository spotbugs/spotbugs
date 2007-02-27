
public class BadShift {
    public static void main(String args[]) {
        short s = (short)0xffff;
        s >>>= 1;
        System.out.println(s);
        byte b = (byte)0xffff;
        b >>>= 1;
        System.out.println(b);
        short[] as = new short[] {(short)0xffff};
        as[0] >>>= 1;
        System.out.println(as[0]);
        byte[] ab = new byte[] {(byte)0xffff};
        ab[0] >>>= 1;
        System.out.println(ab[0]);
        
    }

}
