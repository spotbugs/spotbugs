package npe;

public class BranchOrStatement {
    
    int f(Object x) {
        int result = 0;
        if (x == null) result = 1;
        result += x.hashCode();
        return result;
    }
    int f2(Object x) {
        int result = 0;
        if (x != null) result = 1;
        result += x.hashCode();
        return result;
    }
    int f3(Object x) {
        int result;
        if (x == null) result = x.hashCode();
        else result = x.hashCode();
        return result;
    }

}
