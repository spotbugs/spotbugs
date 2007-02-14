package sfBugs;

public class Bug1645869 extends Object{
	
	/*
	 * Here's the comment from SourceForge.net
	 * The found bug "Method superfluously delegates to parent class method"
	 * reports a method that only calls the super class version of the same
	 * method. In doing so it makes no distinction between a normal method and a
	 * final method. A final method could be a way to impose the default behavior
	 * inherited from super class. Michele. 
	 * 
	 * The bug found is a low priority and FindBugs makes the distinction now. - Kristin
	 */
	
	public static void main(String[] args){
		System.out.println("An attempt to check a bug.");
	}

	public boolean equals(Object obj){
		return super.equals(obj);
	}
	
	public final Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
}
