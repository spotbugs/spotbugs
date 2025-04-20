package ghIssues;

import javax.annotation.Generated;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Issue3396 {
    
    @Generated("test")
    @SuppressFBWarnings
    public boolean equals(Object x) {
    	if (x.toString().equals("")) {
    		return false;
    	}
    	
    	return x == this; 
	}
}