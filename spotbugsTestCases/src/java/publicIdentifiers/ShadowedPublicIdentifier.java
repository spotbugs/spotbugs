package publicIdentifiers;

import java.util.Vector;
public class ShadowedPublicIdentifier {
    private static class Vector {
        private int size = 1;

        public boolean isEmpty() {
            return size == 1;
        }
    }

    void shadowedVector() {
        Vector v = new Vector();
        if (v.isEmpty()) {
            System.out.println("Vector is empty");
        } else {
            System.out.println("Vector is not empty");
        }
    }
}
