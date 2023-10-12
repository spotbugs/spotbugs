package sfBugsNew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature322 {
    @NoWarning("UC_USELESS_VOID_METHOD")
    public void empty() {
    }

    @NoWarning("UC_USELESS_VOID_METHOD")
    public void emptyCaller() {
        empty();
    }

    @ExpectWarning("UC_USELESS_VOID_METHOD")
    public void useless(int a, String b) {
        Object[] obj = new Object[2];
        obj[0] = a;
        obj[1] = b;
    }

    @NoWarning("UC_USELESS_VOID_METHOD")
    public void uselessCaller() {
        useless(1, "");
    }

    @ExpectWarning("UC_USELESS_VOID_METHOD")
    public void uselessCollection(Collection<String> a) {
        List<String> b = new ArrayList<>();
        for(String str : a) {
            if(str.startsWith("b")) {
                b.add(str);
            }
        }
    }

    @NoWarning("UC_USELESS_VOID_METHOD")
    public Object[] returns(int a, String b) {
        Object[] obj = new Object[2];
        obj[0] = a;
        obj[1] = b;
        return obj;
    }

    @ExpectWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @NoWarning("UC_USELESS_VOID_METHOD")
    public void rvIgnored(int a, String b) {
        returns(a, b);
    }

    @ExpectWarning("UC_USELESS_VOID_METHOD")
    public void recursive(int a) {
        if(a > 0) {
            recursive(a-1);
        }
    }

    @ExpectWarning("UC_USELESS_VOID_METHOD")
    public void testFinally(int a) {
        List<Integer> x = new ArrayList<>();
        try {
            x.add(a);
        }
        finally {
            x.add(2);
        }
    }
}
