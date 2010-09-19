package equals;

import javax.annotation.CheckForNull;

public class DelegatingEquals {

    public DelegatingEquals(Object o) {
        this.o = o;
    }

    Object o;

    Object delegate() {
        return o;
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
        return (this == obj) || delegate().equals(obj);
    }

}
