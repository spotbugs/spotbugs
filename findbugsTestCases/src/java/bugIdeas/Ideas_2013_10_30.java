package bugIdeas;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2013_10_30 {

    
    int x;
    
    @NoWarning("SA")
    boolean update(int y) {
        int oldX = x;
        if (y == 1) {
            x = 2;
        } else if (y == 2) {
            x = 3;
        } else {
            x = x*x-x;
    } 
        return x == oldX;
    }
    
    static int a;
    
    @NoWarning("SA")
    static boolean updateStatic(int y) {
        int oldA = a;
        if (y == 1) {
            a = 2;
        } else if (y == 2) {
            a = 3;
        } else {
            a = a*a-a;
    } 
        return a == oldA;
    }
}
