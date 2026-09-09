package multithreaded.compoundoperation;

public class CompoundSubstractionOfArg extends Thread {
    private int num = 0;

    public void toggle(int param) {
        num += param;
    }

    public Integer getNum() {
        return num;
    }
}
