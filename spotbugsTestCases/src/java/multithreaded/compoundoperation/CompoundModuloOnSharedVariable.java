package multithreaded.compoundoperation;

public class CompoundModuloOnSharedVariable implements Runnable {
    private Integer num = 0;

    @Override
    public void run() {
        num %= 2;
    }

    public Integer getNum() {
        return num;
    }
}
