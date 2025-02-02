package multithreaded.compoundoperation;

public class CompoundAdditionWithAnotherVar extends Thread {
    private int num = 0;
    private int num2 = 2;

    public void toggle() {
        num2 = num;
        num += 1;
    }

    public Integer getNum() {
        return num;
    }

    public Integer getNum2() {
        return num2;
    }
}
