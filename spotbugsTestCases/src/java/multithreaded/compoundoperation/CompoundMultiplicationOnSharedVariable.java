package multithreaded.compoundoperation;

public class CompoundMultiplicationOnSharedVariable extends Thread {
    private Integer num = 0;

    public void toggle() {
        num *= 2;
    }

    public Integer getNum() {
        return num;
    }
}
