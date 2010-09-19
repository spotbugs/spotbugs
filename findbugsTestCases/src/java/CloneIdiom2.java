class CloneIdiom2 implements Cloneable {
    public CloneIdiom2 copy() {
        try {
            return (CloneIdiom2) clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
