public class QuestionableBooleanAssignmentInAssertStatement {

    public static void main(String args[]) {
        boolean debug = false;
        assert debug = true;
        System.out.println(debug);
    }
}
