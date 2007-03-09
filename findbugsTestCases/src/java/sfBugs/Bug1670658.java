package sfBugs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Bug1670658 {

public <T extends Collection<Object>> T getCollection(T collection) {
return collection; 
}

public List<Object> getList() {
/* false BC */  return getCollection(new ArrayList<Object>());
}

}
