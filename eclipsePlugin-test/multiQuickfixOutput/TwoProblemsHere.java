import java.util.List;


public class TwoProblemsHere {

    private final String string;


    public TwoProblemsHere(String s) {
        if (s == "bar") {                       //should fix (broken)
            Double d = Double.valueOf(7.1);         //should fix (broken)
            Double d2 = returnQuick();

            System.out.printf("%s %s", d, d2);
        }
        this.string = s;
    }

    private Double returnQuick() {
        return Double.valueOf(7.2);        //should fix (broken)
    }

    private boolean check(String otherValue) {
        return string == otherValue;                //should fix (broken)
    }

    public boolean noProblemsHere(List<String> words) {
        for(String s : words) {
            if (check(s)) {
                return false;
            }
        }
        return true;
    }

}
