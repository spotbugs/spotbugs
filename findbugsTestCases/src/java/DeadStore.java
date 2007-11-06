import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class DeadStore {

	int x, y;

	public static void main(String[] args) {
		String live = "Hello, world";
		String dead = "Oops!";

		System.out.println(live);
		args = new String[0];
	}

	
	public void testDatabaseStore(Connection c) throws SQLException {
		Statement s = c.prepareStatement("select something from other");
		Map m = Collections.emptyMap();
		
	}
	public void storeOfNull() {
		for(int i = 0; i < 10; i++) {
			HashSet<Integer> set = new HashSet<Integer>();
			set.add(i);
			System.out.println(set.hashCode());
			set = null;
		}	
	}

	public int finalLocalDNR(int a) {
		final int SCAN = 0; // <<---- complains about this line
		final int STAR = 1;
		final int DONE = 2;
		// int state = SCAN;

		a += SCAN;
		a += STAR;
		a += DONE;

		return a;
	}

	public void duplicateDeadStores() {
		try {
			Object o = new Object();
		} catch (RuntimeException e) {
		}
		try {
			Object o = new Object();
		} catch (RuntimeException e) {
		}
		try {
			Object o = new Object();
		} catch (RuntimeException e) {
		}
	}

	public int storeNullDNR(int a) {
		Object foo = null;
		return a;
	}

	public int storeZeroDNR(int a) {
		int count = 0;

		return a;
	}

	public int killedByStoreDNR(int a) {
		int b = 3;

		if (a > 1) {
			b = 4;
			a += b;
		}

		return a;
	}

	public int notReportedin086(Object o) {
		if (o instanceof String) {
			String s = (String) o; // Not reported in 0.8.6 but reported in
			// 0.8.5 (Bug: 1105217)
		}
		return o.hashCode();
	}

	public int cachingFields(int a, int b, int c, int d, int e) {
		a = x;
		b = 5;
		c = x + 1;
		d = hashCode();
		return e;
	}


	public enum StateEnum { OK, TIMEDOUT, KILLED, FAILED, OTHER }
	public StateEnum mutateStatusDoNotReport(StateEnum st) {
		return st;
	}
	/** possible false+ reported by Kelly O'Hair */
	void setFailedState(StateEnum orig /*BuildTargetStatus status*/) {
		StateEnum id = mutateStatusDoNotReport(orig); //status.getState().id();
		switch ( id ) {
			case TIMEDOUT:
			case KILLED:
			case FAILED:
					break;
			default:
					System.out.println(orig); //setState(status, new StateID(StateEnum.FAILED));
					break;
			}
		}



}
