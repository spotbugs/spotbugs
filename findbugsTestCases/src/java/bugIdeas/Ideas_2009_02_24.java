package bugIdeas;

import java.util.Arrays;
import java.util.List;

public class Ideas_2009_02_24 {
	
	final static List<String> NAMES = Arrays.asList(new String[] {"John", "Bill", "Sue", "Sarah"});
	
	public static void main(String args[]) {
		falsePositive();
	}
	public static void falsePositive() {
		String [] a = (String[]) NAMES.toArray();
		String [] b = (String[]) Arrays.asList(new String[] {"x","y"}).toArray();
		
	}

}
