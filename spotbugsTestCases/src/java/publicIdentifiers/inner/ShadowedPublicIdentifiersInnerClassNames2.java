package publicIdentifiers.inner;

public class ShadowedPublicIdentifiersInnerClassNames2 {
    public static class List {
        private static class Node {
            public int data;
            public Node() {
                this.data = 5;
            }
        }

        public Node currentNode = new Node();
    }

    public void printInnerData() {
        List myCustomList = new List();
        System.out.println(myCustomList.currentNode.data);
    }
}
