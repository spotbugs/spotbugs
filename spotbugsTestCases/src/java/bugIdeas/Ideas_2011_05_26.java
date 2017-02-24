package bugIdeas;

public class Ideas_2011_05_26 {

    public boolean standardEquals(Object o) {
        return this == o;
        }

    static class Inner extends Ideas_2011_05_26 {
        public boolean equals(Object o) {
            return standardEquals(o);
        }
    }

}
