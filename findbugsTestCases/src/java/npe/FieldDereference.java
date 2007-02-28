package npe;

public class FieldDereference {
    
    String list;
    public FieldDereference(String list, int x) {
      this.list = list;
    }
    
    public int getValue(String one) {
      if (list == null && one != null) {
        list = one;
      }
      return list.hashCode();
    
    }
}
