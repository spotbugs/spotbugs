package sfBugs;

public class Bug1864046 {
    public String m_field1, m_field2, m_field3;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_field1 == null) ? 0 : m_field1.hashCode());
        result = prime * result + ((m_field2 == null) ? 0 : m_field2.hashCode());
        result = prime * result + ((m_field3 == null) ? 0 : m_field3.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Bug1864046 other = (Bug1864046) obj;
        if (m_field1 == null) {
            if (other.m_field1 != null)
                return false;
        } else if (!m_field1.equals(other.m_field1))
            return false;
        if (m_field2 == null) {
            if (other.m_field2 != null)
                return false;
        } else if (!m_field2.equals(other.m_field2))
            return false;
        if (m_field3 == null) {
            if (other.m_field3 != null)
                return false;
        } else if (!m_field3.equals(other.m_field3))
            return false;
        return true;
    }

}
