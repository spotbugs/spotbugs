package npe;

public class FieldDereference {

	String list;
	public FieldDereference(String list) {
      this.list = list;
	}

	public int dereferenceOfField(String one) {
      if (list == null && one != null) {
		list = one;
	  }
	  return list.hashCode();
    }
	public int dereferenceOfVariable(String one, String list) {
		if (list == null && one != null) {
		  list = one;
        }
		return list.hashCode();
	  }
}
