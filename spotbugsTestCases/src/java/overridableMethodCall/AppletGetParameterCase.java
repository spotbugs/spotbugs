package overridableMethodCall;

class Base {
    public String getParameter(String name) {
        return name;
    }
}

public class AppletGetParameterCase extends Base {
    AppletGetParameterCase() {
        getParameter("name");
    }
}
