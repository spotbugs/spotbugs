public class Issue2120 {
    static void method() {
        record MyRecord(int from, int to) { /**/ }
        
        enum MyEnum {
            RED, BLUE;
        }
        
        System.out.println(new MyRecord(1, 2));
        System.out.println(MyEnum.RED);
    }
}
