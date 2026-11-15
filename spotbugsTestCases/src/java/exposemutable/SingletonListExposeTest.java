package exposemutable;

import java.util.Collections;
import java.util.List;

public class SingletonListExposeTest {
    private int[] data = new int[4];
    private static int[] staticData = new int[4];
    private List<int[]> lists = Collections.singletonList(new int[4]);

    public int[] getData() {
        return Collections.singletonList(data).get(0);
    }

    public void setData(int[] a) {
        this.data = Collections.singletonList(a).get(0);
    }

    public static int[] getStaticData() {
        return Collections.singletonList(staticData).get(0);
    }

    public static void setStaticData(int[] a) {
        staticData = Collections.singletonList(a).get(0);
    }

    public int[] getFresh() {
        return Collections.singletonList(new int[4]).get(0);
    }

    public int[] getFromFieldList() {
        return lists.get(0);
    }

    public int[] getFromParameterList(List<int[]> values) {
        return values.get(0);
    }

    public void setFromParameterList(List<int[]> values) {
        this.data = values.get(0);
    }
}
