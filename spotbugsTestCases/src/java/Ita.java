
import java.util.ArrayList;

public class Ita {

    public static void main(String[] args) {
        ArrayList<String> myList = new ArrayList<String>(10);
        myList.add("Yoyoyo");
        Object[] myArray;
        String[] smallArray = new String[0];
        myArray = myList.toArray(smallArray);
    }
}
