package npe;

import java.util.Set;

public class GuaranteedDereferenceInLoop2 {

	Set<String> accumulate(Set<String> accumulator, String [] args) {
		if (accumulator == null)
				System.out.println("Accumulator shouldn't be null");

		// TODO: We should probably generate a warning here
		// even though the NPE only occurs on a path that might be infeasible
		for(int i = 0; i < args.length; i++) 
			accumulator.add(args[i]);
		return accumulator;
	}

}
