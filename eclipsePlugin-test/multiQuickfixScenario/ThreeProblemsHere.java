

public class ThreeProblemsHere {


    public static void main(String[] args) {
        Double d = new Double(8.1);        //should fix (broken)
        Double dokay = Double.valueOf("9.1");  //should not fix (not broken)
        Float f = new Float(8.1f);        //should fix (broken)
        Integer i = new Integer(8);        //should fix (broken)

        System.out.printf("%s %s %s %s", d, dokay, f, i);        //to ignore "not used" warnings
    }

}
