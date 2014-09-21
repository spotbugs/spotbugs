

public class OneProblemHere {

    public static void main(String[] args) {
        Double d = new Double(6.1);        //should fix (broken)
        Double d2 = Double.valueOf("6.2");    //should not fix (not broken)
        Double d3 = Double.valueOf(6.3);   //should not fix (not broken)

        System.out.printf("%s %s %s", d, d2, d3);        //to ignore "not used" warnings
    }


    public OneProblemHere(String s) {
        if (s == "foo") {                   //should not fix (broken, but test case selects not to fix)
            dump("OMG FOO");
        }
    }


    private void dump(String string) {
        //
    }

}
