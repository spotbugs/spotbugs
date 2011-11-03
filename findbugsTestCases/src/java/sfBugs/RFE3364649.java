
package sfBugs;

public class RFE3364649 {
    
    public boolean isEmpty() { return true; }
    public boolean isNotEmpty() { return false; }
    
    public boolean test() {
        return !isEmpty();
    }

}
