package publicIdentifiers;

import java.io.File;
import java.util.ArrayList;

public class ShadowedPublicIdentifiersVariableNames {
    private final static String String = "String";

    public static ArrayList containsShadowedLocalVariable2() {
        ArrayList ArrayList = new ArrayList();
        ArrayList.clear();
        ArrayList.add("String");
        return ArrayList;
    }

    public void containsShadowedLocalVariable1() {
        Integer Integer = 1;
        System.out.println(Integer);
    }

    protected void containsShadowedLocalVariable3(File File) {
        System.out.println(File.getName());
    }
}
