package ghIssues;

public class Issue3170Tool {

    public void leak() throws Exception {
        Issue3170MyConnection connection = Issue3170MyConnection.connect();
        System.out.println(connection);
    }
}
