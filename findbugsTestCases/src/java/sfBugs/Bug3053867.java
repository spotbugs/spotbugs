package sfBugs;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3053867 {

    static class Foo {
        int x;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
    }

    HttpSession session;

    @NoWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
    public void setSession(HttpSession session) {
        this.session = session;
    }

    @NoWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
    public void storeMap(Map<String, String> map) {
        session.setAttribute("map", map);
    }

    @NoWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
    public void storeSet(Set<String> set) {
        session.setAttribute("set", set);
    }

    @ExpectWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
    public void storeFoo(Foo foo) {
        session.setAttribute("foo", foo);
    }

    @ExpectWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
    public void storeFooSet(Set<Foo> fooSet) {
        session.setAttribute("fooSet", fooSet);
    }
    @ExpectWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
    public void storeFooMap1(Map<Foo,String> fooMap) {
        session.setAttribute("fooMap", fooMap);
    }
    @ExpectWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
    public void storeFooMap2(Map<String,Foo> fooMap) {
        session.setAttribute("fooMap", fooMap);
    }

}
