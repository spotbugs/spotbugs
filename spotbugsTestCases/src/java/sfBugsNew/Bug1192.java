package sfBugsNew;

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1192 {

    void mightThrow() {

    }

    void mightThrow2() throws IOException, InterruptedException {

    }


    @NoWarning("BC_IMPOSSIBLE_INSTANCEOF")
    void check() {
        try {
            mightThrow();
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            if (e instanceof IndexOutOfBoundsException) {
                System.out.println("IndexOutOfBoundsException");
            } else {
                System.out.println("NullPointerException");
            }
        }
    }

    @NoWarning("BC_IMPOSSIBLE_INSTANCEOF")
    void check2() {
        try {
            mightThrow2();
        } catch (IOException | InterruptedException e) {
            if (e instanceof IndexOutOfBoundsException) {
                System.out.println("IOException");
            } else {
                System.out.println("InterruptedException");
            }
        }
    }
}
