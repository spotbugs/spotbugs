package sfBugs;

public class Bug3358161 {

    public void setAvailableItems(String s, boolean b) {

    }

    public void resetAvailableList(String s, boolean b) {
        s = "foo";
        setAvailableItems(s, !b);
    }

}
