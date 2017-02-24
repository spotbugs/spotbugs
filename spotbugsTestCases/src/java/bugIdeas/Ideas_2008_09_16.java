package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2008_09_16 {

    @Override
    @ExpectWarning("Eq")
    public boolean equals(Object o) {
        if (o instanceof Integer) {
            return o.toString().equals(o);
        }
        if (Double.class.isInstance(o))
            return false;
        if (o instanceof Float)
            return Float.class.cast(o).toString().equals(this.toString());
        return false;
    }

}
