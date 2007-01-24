import java.util.ArrayList;


public class BadDowncastOfToArray {
	
	ArrayList<Integer> lst = new ArrayList<Integer>();
	
	public Integer[] asArray() {
		return (Integer[]) lst.toArray();
	}
	
	@Override
    public boolean equals(Object o) {
		return lst.equals(((BadDowncastOfToArray)o).lst);
	}

}
