package overridableMethodCall;

import java.applet.Applet;

public class AppletGetParameterCase extends Applet {
    AppletGetParameterCase() {
        getParameter("name");
    }
}