package multithreaded.compoundOperationOnSharedVariables;

public class CompoundXOROperationOnSharedVariable extends Thread {
    private Boolean flag = true;

    public void toggle(Boolean tmp) {
        flag ^= tmp;
    }

    public boolean getFlag() {
        return flag;
    }
}