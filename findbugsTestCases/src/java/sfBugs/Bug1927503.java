package sfBugs;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class Bug1927503 {

    @SuppressWarnings("EI2")
    private byte[] arr;

    private byte[] arr2;

    public Bug1927503(@SuppressWarnings("EI2") byte[] newArr) {
        arr = newArr;
    }

    public void setArr2(@SuppressWarnings("EI2") byte[] newArr) {
        arr2 = newArr;
    }

}
