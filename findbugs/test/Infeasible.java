
class Infeasible {

  int foo() {
	Infeasible a,b;
	a = null;
	try {
		a = new Infeasible();
		b = (Infeasible) a.clone();
		}
	catch (CloneNotSupportedException e) {
		e.printStackTrace();
		}
	// a should also be initialized here
	return a.hashCode();
	} 
}
		
		
