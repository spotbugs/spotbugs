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
				something();
				break;
			case B:
				//Nothing to report
				break;
			case C:
				something();
				break;
			case D:
				something();
				break;
			default:
				something();
				break;
		}
	}

	private void something() {
	}
}
