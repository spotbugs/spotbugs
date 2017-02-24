package sfBugs;

import java.util.Arrays;

import org.apache.commons.lang.builder.EqualsBuilder;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2922917 {
    int x, y, z;

    @Override
    public int hashCode() {
        return x + y + z;
    }

    static class A extends Bug2922917 {

        @NoWarning("EQ_UNUSUAL")
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            A rhs = (A) obj;
            return new EqualsBuilder().appendSuper(super.equals(obj)).append(x, rhs.x).append(y, rhs.y).append(z, rhs.z)
                    .isEquals();
        }

    }

    static class B extends Bug2922917 {

        @NoWarning("EQ_UNUSUAL")
        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

    }

    static class C extends Bug2922917 {

        @NoWarning("EQ_UNUSUAL")
        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, new String[] { "x", "y" });
        }

    }

    static class D extends Bug2922917 {

        @NoWarning("EQ_UNUSUAL")
        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, Arrays.asList(new String[] { "x", "y" }));
        }

    }

    static class E extends Bug2922917 {

        @NoWarning("EQ_UNUSUAL")
        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, false);
        }

    }

    static class F extends Bug2922917 {

        @NoWarning("EQ_UNUSUAL")
        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, false, Bug2922917.class);
        }

    }

    static class G extends Bug2922917 {

        @NoWarning("EQ_UNUSUAL")
        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, false, Bug2922917.class, new String[] { "x", "y" });
        }

    }
}
