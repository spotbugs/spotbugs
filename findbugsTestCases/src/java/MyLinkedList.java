
//FIXME This should inform me that making Node static would be a good idea, but it doesn't.
public class MyLinkedList {

    Node head;

    Node tail;

    private class Node {
        int value;

        Node next;

        Node(int x) {
            value = x;
        }
    }

    public MyLinkedList() {
        head = new Node(0);
        tail = head;
    }

    public void add(int x) {
        Node temp = tail;
        tail = new Node(x);
        temp.next = tail;
    }
}
