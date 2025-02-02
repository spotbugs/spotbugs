package multithreaded.compoundoperation;

public class CompoundAdditionComplexExpressionWithAnotherVar extends Thread {
    private int num = 0;
    private int num2 = 2;

    public void toggle() {
        num += num2 + 5;
    }

    public Integer getNum() {
        return num;
    }

    public Integer getNum2() {
        return num2;
    }
}
