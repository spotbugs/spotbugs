package sfBugs;

public class Bug3358161 {

    public void setAvailableItems(String s, boolean b) {

    }

    public void resetAvailableList(String s, boolean b) {
        String t = "foo";
        setAvailableItems(s, !b);
    }

}
