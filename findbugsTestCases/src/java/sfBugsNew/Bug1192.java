package sfBugsNew;

public class Bug1192 {

    void mightThrow() {

    }

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
}