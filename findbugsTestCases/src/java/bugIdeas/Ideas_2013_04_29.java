package bugIdeas;

public class Ideas_2013_04_29 {
    
    public int testSelfOperation(int x) {
        
        int y = x;
        y = y - x;
        return y;
    }

    int a,b;
    public int testSelfOperationField(int x) {
        
        a = x;
        b = a;
        b = b - a;
        return b;
    }

}
