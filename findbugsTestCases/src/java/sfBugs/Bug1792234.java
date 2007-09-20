package sfBugs;

/**
 * Submitted By: Jarek Pawlak
 * Summary:
 * 
 * The rule FI: Explicit invocation of finalizer reports false positive
 * encountering a call to method "finalize" with signature other than
 * Object.finalize().
 * i.e. the rule will report a violation on a call to e.g.
 * finalize(String,List<Object>) - a self defined method.
 */
public class Bug1792234 {
    
    public static void main(String args[]) {
        Bug1792234 b = new Bug1792234();
        try {
            b.finalize("are here again");
        } catch(Throwable t) { }
    }
    
    //// grep -A 1 FI_EXPLICIT_INVOCATION | grep Bug1792234 
    protected void finalize(String s) throws Throwable {
        System.out.println("Happy days " + s);
    }
}
