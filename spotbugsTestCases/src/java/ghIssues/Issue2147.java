package ghIssues;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Issue2147 {
    private Object object = new Object();

    private static final List<String> PIN_LIST = Collections
            .unmodifiableList(Arrays.asList("1", "2", "3", "4"));

    public static void fillConstantAssignments(Map<String, String> map, Issue2147A state) {
    	Issue2147B[] assignments = state.info.getAssignments();

        for (Issue2147B assignment : assignments) {
            StringBuilder key = new StringBuilder();
            if (PIN_LIST.contains(assignment.getId())) {
                key.append("p");
            }
            map.put(key.toString(), null);
        }
    }

    public void method1() {
        object = new Object();
    }

    public Object getObject() {
        return object;
    }
}

class Issue2147A {
	Issue2147C info = new Issue2147C();
}

class Issue2147B {
	public String getId() {
		return "";
	}
}

class Issue2147C {
	Issue2147B[] getAssignments() {
		return new Issue2147B[0];
	}
}
