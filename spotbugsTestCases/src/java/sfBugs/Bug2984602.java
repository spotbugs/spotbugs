package sfBugs;

public class Bug2984602 {

    int itemID;

    @Override
    public int hashCode() {
        return itemID;
    }

    @Override
    public boolean equals(Object object) {
        if (!(Bug2984602.class.isInstance(object))) {
            return false;
        }

        boolean equals = false;
        Bug2984602 compareTo = (Bug2984602) object;

        if (this.itemID == compareTo.itemID) {
            equals = true;
        }

        return equals;
    }

}
