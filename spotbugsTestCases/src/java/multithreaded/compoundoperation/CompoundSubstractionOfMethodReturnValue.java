package multithreaded.compoundoperation;

public class CompoundSubstractionOfMethodReturnValue extends Thread {
    private int num = 0;

    public void toggle() {
        num += getOne();
    }

    public Integer getNum() {
        return num;
    }

    private int getOne() {
        return 1;
    }
}
