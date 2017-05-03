import java.util.Calendar;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/* From "More Programming Puzzlers" */

public class Elvis {
    @ExpectWarning("SI_INSTANCE_BEFORE_FINALS_ASSIGNED")
    public static final Elvis INSTANCE = new Elvis();

    private final int beltSize;

    private static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    private Elvis() {
        beltSize = CURRENT_YEAR - 1930;
    }

    public int beltSize() {
        return beltSize;
    }

    public static void main(String args[]) {
        System.out.println("Elvis wears size " + INSTANCE.beltSize() + " belt.");
    }
}
