package deref;

import java.util.Set;

public class UnconditionalDereferenceInLoop {
	
	Set<String> accumulate(Set<String> accumulator, String [] args) {
		if (accumulator == null)
				System.out.println("Accumulator shouldn't be null");
		
		// TODO: We should probably generate a warning here
		// even though the NPE only occurs on a path that might be infeasible
		
		for(String s : args) 
			accumulator.add(s);
		return accumulator;
	}

}
