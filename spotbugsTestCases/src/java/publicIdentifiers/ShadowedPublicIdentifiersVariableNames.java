package publicIdentifiers;

import java.io.File;
import java.util.ArrayList;

public class ShadowedPublicIdentifiersVariableNames {
    public static ArrayList containsShadowedLocalVariable1() {
        ArrayList ArrayList = new ArrayList();
        ArrayList.clear();
        ArrayList.add("String");
        return ArrayList;
    }

    public void containsShadowedLocalVariable2() {
        Integer Integer = 1;
        System.out.println(Integer);
    }

    protected void containsShadowedLocalVariable3(File File) {
        System.out.println(File.getName());
    }
}
