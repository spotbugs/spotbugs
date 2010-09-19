package sfBugs;

public class Bug1926389 {
    public int avg(int i, int j, int[] elts) {
        int k = (i + j) / 2;
        System.out.println(k);
        return elts[k];
    }
}
