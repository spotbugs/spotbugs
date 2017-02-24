package sfBugsNew;

public class Bug1209 {
    public double getValue(Object obj) {
        if(obj instanceof Number || obj instanceof String) {
            if(obj instanceof Number) {
                return ((Number)obj).doubleValue();
            } else {
                return Double.parseDouble((String)obj);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
