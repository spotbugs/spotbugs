package multithreaded.compoundoperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompoundXOROperationOnSharedVariable {
    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>()); // just to signal that its multithreaded
    private Boolean flag = true;

    public void toggle(Boolean tmp) {
        flag ^= tmp;
    }

    public boolean getFlag() {
        return flag;
    }
}