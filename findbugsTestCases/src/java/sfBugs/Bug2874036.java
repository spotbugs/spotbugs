package sfBugs;

import java.util.ResourceBundle;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug2874036 {
    private static ResourceBundle createRb() {
        return null;
    }

    private final ResourceBundle rb = createRb();

    @DesireNoWarning("NP")
    public String getText(String key) {
        String res = rb.getString(key);
        if (res == null) {
            System.out.println("getText: no language entry for '" + key + "'");
        }
        return res != null ? res : key;
    }
    
    public String getText2(String key) {
        String res = rb.getString(key);
        return res != null ? res : key;
    }

}
