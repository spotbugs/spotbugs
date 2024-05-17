public class RefComparisons {
    
    public static boolean integerBadComparison(Integer a, Integer b) {
        return a == b;
    }

    public class MyClass1 {
    }

    public static boolean myClass1BadComparison(MyClass1 a, MyClass1 b) {
        return a == b;
    }

    public class MyClass2 {
    }

    public static boolean myClass2BadComparison(MyClass2 a, MyClass2 b) {
        return a == b;
    }
}
