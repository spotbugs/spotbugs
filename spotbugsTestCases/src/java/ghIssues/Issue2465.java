package ghIssues;

public class Issue2465 {
	private boolean flag;
	private Role role;

	public void waitForEB() {
		flag = false;
		
		if (this.role == null) {
			this.role = new Role();
		}
		
		do {
			flag = this.role.check();
		} while (!flag);
	}
	
	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	private class Role {
		public boolean check() {
			return false;
		}
	}
}
