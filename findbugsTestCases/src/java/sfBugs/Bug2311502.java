package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug2311502 {

	/**
	 * Should flag code as unsafe. 
	 */
	static public class NonNullFalseNegative {
	    
	    @CheckForNull
	    private Object junkField;
	    
	    public void setJunk(Object junk) {
	        this.junkField = junk;
	    }
	    
	    public final class BadInnerClass {
	        public void badMethod() {
	            System.out.println(junkField.hashCode()); // should be caught as a bug 
	        }
	    }
	    
	}
	@DefaultAnnotation(NonNull.class)
	static public class NPNonNullReturnViolationBug {
	    
	    @CheckForNull
	    private Object junkField;
	    
	    public void setJunk(Object junk) {
	        this.junkField = junk;
	    }

	    public final class InnerClass {
	        /**
	         * Prints out {@link NPNonNullReturnViolationBug#junkField}, 
	         * if it's currently not <code>null</code>.
	         */
	        public void printJunk() {
	            Object temp = junkField;
	            if (temp != null) { // should be perfectly safe
	                System.out.println(temp.hashCode());
	            }
	        }
	    }

	}


}
