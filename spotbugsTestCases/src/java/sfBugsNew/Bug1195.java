package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1195 {

    @ExpectWarning("DM_NUMBER_CTOR")
    public String test1(int myInt) {
        return new Integer(myInt).toString();
    }
    
    public String test1a(int myInt) {
        return ((Integer) myInt).toString();
    }

    public String test1Good(int myInt) {
        return Integer.toString(myInt);
    }

    @ExpectWarning("DM_NUMBER_CTOR")
    public int test2(String myString) {
        return new Integer(myString).intValue();
    }

    public int test2Good(String myString) {
        return Integer.parseInt(myString);
    }
    
    @ExpectWarning("DM_NUMBER_CTOR")
    public Integer test3(String myString) {
        return new Integer(myString);
        
    }
    public Integer test3Good(String myString) {
        return Integer.valueOf(myString);
        
    }
}
