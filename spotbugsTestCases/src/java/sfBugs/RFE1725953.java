package sfBugs;

public class RFE1725953 {
    public static void main(String args[]) {
        int twoToThe16 = 2 ^ 16;
        System.out.println("2 ^ 16 = " + twoToThe16); // bytecode just has 18
    }
}
