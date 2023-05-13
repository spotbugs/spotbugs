package publicIdentifiers;

import publicIdentifiers.standalone.InputStream;

import java.util.ArrayList;

public class ShadowedPublicIdentifiersVariableNames {
    private final String String = "String";

    public void containsShadowedLocalVariable1() {
        Integer Integer = 1;
    }

    public void containsShadowedLocalVariable2() {
        ArrayList ArrayList = new ArrayList();
    }
}
