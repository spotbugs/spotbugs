package publicIdentifiers;

import publicIdentifiers.standalone.InputStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ShadowedPublicIdentifiersVariableNames {
    private final static String String = "String";

    public void containsShadowedLocalVariable1() {
        Integer Integer = 1;
        System.out.println(Integer);
    }

    public static ArrayList containsShadowedLocalVariable2() {
        ArrayList ArrayList = new ArrayList();
        ArrayList.clear();
        ArrayList.add("String");
        return ArrayList;
    }

    private void containsShadowedLocalVariable3(File File) {
        System.out.println(File.getName());
    }
}
