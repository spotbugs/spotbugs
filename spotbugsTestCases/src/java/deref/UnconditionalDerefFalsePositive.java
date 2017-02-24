package deref;

// Based on code from org.apache.tools.zip.ZipEntry
public class UnconditionalDerefFalsePositive {
    int x;

    UnconditionalDerefFalsePositive(int x) {
        this.x = x;
    }

    @Override
    public Object clone() {
        UnconditionalDerefFalsePositive e = null;
        try {
            e = (UnconditionalDerefFalsePositive) super.clone();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        e.x = 1;
        return e;
    }

}
