package sfBugs;

public class RFE3026970 {
    private String get() {
        return null;
    }

    public void testDesireWarning() {
        if (get().equals("A"))
            System.out.println("test");
    }

    public void testDesireNoWarning() {
        if ("A".equals(get()))
            System.out.println("test");
    }
}
