package npe;

public class GuaranteedFieldDereference4 {
	static class Node {
		Object value;
		Node next;
		void setNext(Node n) {
			next = n;
		}
	}
	Node propertyListTail, propertyListHead;
	
		 void falsePositive(Node prop) {
			if (propertyListTail != null) {
				propertyListTail.next = prop;
				propertyListTail = prop;
			} else {
				propertyListHead = propertyListTail = prop;
			}
			prop.next = prop;
		}
}
