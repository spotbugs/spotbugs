package publicIdentifiers.standalone;

import java.util.Vector;

public class GoodPublicIdentifiersStandaloneClassNames {
    void fullReferenceToVector() {
        java.util.Vector v = new java.util.Vector();
        if (v.isEmpty()) {
            System.out.println("Vector is empty");
        } else {
            System.out.println("Vector is not empty");
        }
    }

    void simpleNameReferenceToVector() {
        Vector v = new Vector();
        if (v.isEmpty()) {
            System.out.println("Vector is empty");
        } else {
            System.out.println("Vector is not empty");
        }
    }
}
