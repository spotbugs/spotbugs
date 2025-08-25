package ghIssues;

public class Issue2258 {
	private boolean isHole;
	private Issue2258 next;
	
	public void fixHoleLinkage() {
		Issue2258 orfl = next;
		
		while (orfl != null && (orfl.isHole == isHole)) {
			orfl = orfl.next;
		}
		
		next = orfl;
	}
	
	public void setNext(Issue2258 next) {
		this.next = next;
	}
	
	public void setHole(boolean isHole) {
		this.isHole = isHole;
	}
}
