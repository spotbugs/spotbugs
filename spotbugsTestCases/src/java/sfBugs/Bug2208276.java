package sfBugs;

public class Bug2208276 {
    char[][] matrix1 = new char[0][];

    public void falsePositive() {
        char[][] matrix2 = new char[0][];
        if (matrix1 == matrix2) {
            System.out.println("hoho");
        }
    }
}
