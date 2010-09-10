package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3062023 {
	private enum E {
		A, B, C, D
	}

	@DesireNoWarning("DB_DUPLICATE_SWITCH_CLAUSES")
	private void noWarning(E e) {
		switch (e) {
			case A:
				something(1);
				break;
			case B:
				//Nothing to report
				break;
			case C:
				something(2);
				break;
			case D:
				break;
			default:
				something(10);
				break;
		}
	}

	private void something(int i) {
	}
}
