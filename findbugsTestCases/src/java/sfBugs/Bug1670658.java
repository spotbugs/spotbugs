package sfBugs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1670658 {

    @NoWarning("BC")
    public <T extends Collection<Object>> T getCollection(T collection) {
        return collection;
    }

    @NoWarning("BC")
    public List<Object> getList() {
        return getCollection(new ArrayList<Object>());
    }

}
