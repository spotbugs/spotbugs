package npe;

public class InterproceduralAnalysisOfNonnullReturnValues {
    
    String f() {
        return "x";
    }
    int g() {
        String s = f();
        if (s == null)
            return s.hashCode();
        return 0;
    }

}
