package ghIssues;

public class Issue1405 {

    public String greeting = "hey";

    private Issue1405 another;

    public Issue1405 getAnother() {
        return another;
    }

    public void setAnother(Issue1405 another) {
        this.another = another;
    }

    public static void main(String[] args) {
        Issue1405 some = new Issue1405();
        System.out.println(some.getAnother().greeting);
    }
}
