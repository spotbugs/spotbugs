package sfBugs;

import java.util.ArrayList;
import java.util.List;

public class Bug1611715 {

	List lst = new ArrayList();

	List method(){
	return (List) ((ArrayList)lst).clone();
	}
}
