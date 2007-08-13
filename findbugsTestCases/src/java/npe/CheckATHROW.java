package npe;

public class CheckATHROW {
	public void doNotReport(Throwable obj) throws Throwable {
		throw obj;
	}
	
	public void throwNullValueReport(boolean b) throws Throwable {
		
		Throwable t = null;
		
		if (b) {
			System.out.println("Yeah");
		}
		
		throw t;
	}
	
	public void throwPossiblyNullReport(Throwable t) throws Throwable {
		if (t != null) {
			System.out.println("ok");
		}
		
		throw t;
	}
}
