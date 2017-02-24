import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class BadDowncastOfToArray {

    ArrayList<Integer> lst = new ArrayList<Integer>();

    @ExpectWarning("BC")
    public Integer[] asArray() {
        return (Integer[]) lst.toArray();
    }

    @Override
    @ExpectWarning("BC")
    public boolean equals(Object o) {
        return lst.equals(((BadDowncastOfToArray) o).lst);
    }

}
