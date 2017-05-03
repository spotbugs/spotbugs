
import java.io.IOException;
import java.io.InputStream;

public class Finalizer {

    /**
     * @param args
     */
    public static void main(String[] args) {

    }

    class Parent {
        InputStream f;

        @Override
        protected void finalize() {
            // System.out.println("I'M MEEELLLLTTTTINNNNGGGGG");
            try {
                f.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    class Child extends Parent {
        @Override
        protected void finalize() {
            System.out.println("nooooooooo");
        }
    }

    class Deviant extends Parent {
        @Override
        protected void finalize() {
        }
    }
}
