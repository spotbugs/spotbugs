package npe;

public class GuaranteedFieldDereference4 {
    static class Node {
        public Object value;

        public Node next;
    }

    public Node propertyListTail, propertyListHead;

    void falsePositive(Node prop) {
        if (propertyListTail != null) {
            propertyListTail.next = prop;
            propertyListTail = prop;
        } else {
            propertyListHead = propertyListTail = prop;
        }
        prop.next = null;
    }
}
