package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class RFE3285242 {
    protected String theDing;

    public String getTheDing() {
        return theDing;
    }

    public void setTheDing(String theDing) {
        this.theDing = theDing;
    }

    public static class Dong extends RFE3285242 {
        @ExpectWarning("MF_CLASS_MASKS_FIELD")
        protected String theDing;

        public String getTheDing() {
            return theDing;
        }

        public void setTheDing(String theDing) {
            this.theDing = theDing;
        }
    }

}
