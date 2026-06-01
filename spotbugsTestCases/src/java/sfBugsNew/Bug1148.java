package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1148 {

    @ExpectWarning("NP_NULL_INSTANCEOF,BC_IMPOSSIBLE_INSTANCEOF")
    public static void main(String[] args) {

        byte[] msg = null;

        if (msg instanceof byte[]) {
            System.out.println("this is a byte[]");
        } else {
            System.out.println("this is not a byte[]");
        }

        msg = "hello".getBytes();

        if (msg instanceof byte[]) {
            System.out.println("this is a byte[]");
        } else {
            System.out.println("this is not a byte[]");
        }

        msg = new byte[100];

        if (msg instanceof byte[]) {
            System.out.println("this is a byte[]");
        } else {
            System.out.println("this is not a byte[]");
        }
    }
}
