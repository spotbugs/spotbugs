package sfBugs;

import java.io.FilterInputStream;

public class Bug3118278 {
    public static void main(String[] args) {
        if (System.in instanceof FilterInputStream) {
//            System.out.println("hi");
        }
    }

}
