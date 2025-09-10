package exposemutable;

public class InnerField {
    public static class PreferenceTreeNode {
        private PreferenceTreeNode fParent;
        public void addChild(PreferenceTreeNode node) {
            node.fParent= this;
        }
        public PreferenceTreeNode getfParent() {
            return fParent;
        }
    }
}
