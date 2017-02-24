package sfBugs;

public class Bug1911766 {

    public int test(Object var1, Object var2) {
        int returnValue;
        if ((var1 == null) && (var2 == null))
            returnValue = 0;
        else if (var1 == null) // [warns here]
            returnValue = -1;
        else if (var2 == null)
            returnValue = 1;
        else {
            returnValue = 0;
        }
        return returnValue;
    }
}
