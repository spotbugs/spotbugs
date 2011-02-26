package sfBugs;

public class Bug3180721 {
    public String myMethod(String formBe, Integer typeDecl) {
        String error = "";

        switch (typeDecl) {
            case 1:
                if (formBe.isEmpty()) {
                    error = formBe + ".";
                }
                break;

            case 2:
                if (formBe.isEmpty()) {
                    error = formBe + "x";
                }
                break;
            case 8:
                if (formBe.isEmpty()) {
                    // SF_SWITCH_NO_DEFAULT false positive on the line below:
                    error = formBe;
                }
            default:
                break;
        }

        return error;
    }
}
