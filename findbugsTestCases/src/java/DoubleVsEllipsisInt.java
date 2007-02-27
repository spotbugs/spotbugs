public class DoubleVsEllipsisInt {
    public DoubleVsEllipsisInt() {
    }
    public void method(int ...intargs) {
        System.out.println("ellipsis int");
    }
    public void method(double dblarg) {
        System.out.println("double");
    }
   public static void main(String[] args) {
       DoubleVsEllipsisInt obj=new DoubleVsEllipsisInt();
       Integer i=1;
       obj.method(i);
       obj.method((int)i);
       obj.method(1);
       obj.method(i,0);
   }
}