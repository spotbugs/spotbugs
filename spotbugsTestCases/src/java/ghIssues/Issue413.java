package ghIssues;

public class Issue413 {

	public long error_in_ldiv() {
		return 1L / (1L / 2);
	}

	public long error_in_lrem() {
		return 1L % (1L / 2);
	}

}
