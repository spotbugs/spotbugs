package ghIssues;

public class Issue4295 {

    public void impossibleNullCondition(Object value) {
        if (value == null && value != null) {
            int[] array = new int[1];
            array[2] = 1;
        }
    }

    public void reachableNullCheckBody(Object value) {
        if (value == null) {
            int[] array = new int[1];
            array[2] = 1;
        }
    }

    public void explicitNullNoAccess() {
        Object value = null;
        if (value != null) {
            int[] array = new int[1];
            array[2] = 1;
        }
    }
}
