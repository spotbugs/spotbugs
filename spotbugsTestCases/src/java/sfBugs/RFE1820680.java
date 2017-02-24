package sfBugs;

public class RFE1820680 {

    static class Array1 {
        Array2[] getArray2() {
            return null;
        }
    }

    static class Array2 {
    }

    public void test() {
        Array1[] array1 = getArray1();
        for (int i = 0; i < array1.length; i++) {
            System.out.println("array 1:" + array1[i]);
            Array2[] array2 = array1[i].getArray2();
            for (int j = 0; j < array2.length; j++) {
                System.out.println("array 2:" + array2[i]); // should be j
            }
        }
    }

    private Array1[] getArray1() {
        return null;
    }

    void te() {
        String str = "Pushpendra";
        for (int i = 0; i < str.length(); i++) {
            String str1 = "Japan";
            for (int j = 0; j < str1.length(); j++) {
                System.out.println(i);
            }
        }
    }
}
