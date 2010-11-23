package sfBugs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Bug3110239 implements Serializable {
    public Bug3110239(String id, String name, Bug3110239 parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
    }
    private static final long serialVersionUID = 9L;
    public static final String ROOT_NAME = "root";
    private final String id;
    private final String name;
    private final Bug3110239 parent;

    private List<Bug3110239> subSectors = new ArrayList<Bug3110239>();
    private Set<String> companies = new HashSet<String>();
    
}
