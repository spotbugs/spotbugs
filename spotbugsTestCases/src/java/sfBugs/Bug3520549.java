package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3520549 {

    static class Thing {
        static class One extends Thing {
        }

        static class Two extends Thing {
        }

        static class Three extends Thing {
        }
    }

    Thing thing;

    Bug3520549(Thing thing) {
        this.thing = thing;
    }

    @NoWarning("BC_UNCONFIRMED_CAST")
    void okay(Thing thing) {
        if (thing instanceof Thing.One) {
            one((Thing.One) thing);
        }
        if (thing instanceof Thing.Two) {
            two((Thing.Two) thing);
        }
        if (thing instanceof Thing.Three) {
            three((Thing.Three) thing);
        }
    }

    @NoWarning("BC_UNCONFIRMED_CAST")
    void notOkay() {
        if (thing instanceof Thing.One) {
            one((Thing.One) thing);
        }
        if (thing instanceof Thing.Two) {
            two((Thing.Two) thing); // false+ BC_UNCONFIRMED_CAST
        }
        if (thing instanceof Thing.Three) {
            three((Thing.Three) thing); // false+ BC_UNCONFIRMED_CAST
        }
    }

    void one(Thing.One thing) {
    }

    void two(Thing.Two thing) {
    }

    void three(Thing.Three thing) {
    }

}
