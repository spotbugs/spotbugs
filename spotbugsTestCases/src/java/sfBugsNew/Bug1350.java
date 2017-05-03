package sfBugsNew;

import java.net.MalformedURLException;
import java.net.URL;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1350 {
    Integer field;

    @NoWarning("NP_NULL_ON_SOME_PATH_EXCEPTION")
    public String get() {
        if(field == null) {
            try {
                initField();
            } catch (IllegalStateException e) {
                // ignore
            }
        }
        return field.toString();
    }

    private void initField() {
        if(field != null)
            throw new IllegalStateException();
        field = 0;
    }
    
    Object obj;
    
    @NoWarning("NP_ALWAYS_NULL_EXCEPTION")
    public void test() {
        if(obj == null) {
            try {
                initObj();
            }
            catch(MalformedURLException ex) {
                System.err.println(obj.toString());
            }
        }
    }

    private void initObj() throws MalformedURLException {
        try {
            obj = new URL("test:///");
        } catch (MalformedURLException e) {
            obj = e;
            flag = field != null;
            throw e;
        }
    }
    
    boolean flag = false;
    @NoWarning("UC_USELESS_CONDITION")
    public void ucTest() {
        if(!flag) {
            try {
                initObj();
            }
            catch(MalformedURLException e) {
                if(flag) {
                    System.out.println("test");
                }
            }
        }
    }
}
