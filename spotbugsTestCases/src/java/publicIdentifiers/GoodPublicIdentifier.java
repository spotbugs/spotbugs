package publicIdentifiers;

import java.util.Vector;
import static java.lang.System.out;
public class GoodPublicIdentifier {
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

    void staticImportVariable() {
        String System = "System";
        out.println(System);
    }
}
