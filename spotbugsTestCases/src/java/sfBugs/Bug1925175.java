package sfBugs;

public class Bug1925175 {
    public static final class Foo {
        private String m_str;

        public void setString(String s) {
            m_str = s;
        }

        public String getPrefix() {
            return m_str.substring(0, 1);
        }
    }

    public static final class Bar {
        private String m_str;

        public void setString(String s) {
            m_str = s;
        }

        public String getPrefix() {
            assert null != m_str;
            return m_str.substring(0, 1);
        }
    }

}
