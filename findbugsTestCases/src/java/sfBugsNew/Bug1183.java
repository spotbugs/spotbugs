package sfBugsNew;

public class Bug1183 {
    
    public static int diveByZeroConstant() {
        return 1/0;
    }
    
    public static int diveByZeroDataflow() {
        int x = 0;
        return 1/x;
    }
    
    public static int diveByZeroConditionalTest(int x) {
        if (x == 0)
            return 1/x;
        return x;
    }


}
