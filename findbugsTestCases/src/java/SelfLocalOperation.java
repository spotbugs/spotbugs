
public class SelfLocalOperation {
    
    int f(int x, int y) {
        if (x < x)
            x = y^y;
        if (x != x)
            y = x|x;
        if (x >= x)
            x = y&y; 
        return x; 
    }
    
    long f(long x, long y) {
        if (x < x)
            x = y^y;
        if (x != x)
            y = x|x;
        if (x >= x)
            x = y&y; 
        return x; 
    }
    
    boolean e(Object x, Object y) {
        return x.equals(x);
    }
    int c(Integer x, Integer y) {
        return x.compareTo(x);
    }
    

}
