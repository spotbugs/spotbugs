package ghIssues;

import java.util.List;
import java.util.Map;

/**
 * Fields and record components that keep the {@code null} of a parameter and otherwise receive an unmodifiable copy
 * of it do not expose internal representation (<a href="https://github.com/spotbugs/spotbugs/issues/4033">#4033</a>).
 */
public final class Issue4033 {

    public record Feature(String id, Map<String, Object> properties, List<Double> bbox, Map<String, Object> foreignMembers) {
        public Feature {
            if (bbox != null) {
                bbox = List.copyOf(bbox);
            }
            if (properties != null) {
                properties = Map.copyOf(properties);
            }
            if (foreignMembers != null) {
                foreignMembers = Map.copyOf(foreignMembers);
            } else {
                foreignMembers = Map.of();
            }
        }
    }

    public record MultiPolygon(List<List<List<String>>> coordinates, List<Double> bbox) {
        public MultiPolygon {
            if (coordinates != null) {
                coordinates = coordinates.stream().map(l -> l.stream().map(List::copyOf).toList()).toList();
            } else {
                coordinates = List.of();
            }
            if (bbox != null) {
                bbox = List.copyOf(bbox);
            }
        }
    }

    /** The same shape in an ordinary class, with both forms of the null check. */
    public static final class Holder {
        private final List<String> names;
        private final Map<String, String> attributes;

        public Holder(List<String> names, Map<String, String> attributes) {
            if (names != null) {
                names = List.copyOf(names);
            }
            if (attributes == null) {
                this.attributes = null;
            } else {
                this.attributes = Map.copyOf(attributes);
            }
            this.names = names;
        }

        public List<String> getNames() {
            return names;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }
    }

    /** Storing a parameter that is known to be null does not expose anything either. */
    public static final class NullStore {
        private final List<String> names;

        public NullStore(List<String> names) {
            if (names == null) {
                this.names = names;
            } else {
                this.names = List.copyOf(names);
            }
        }

        public List<String> getNames() {
            return names;
        }
    }

    /** A null check that does not copy the parameter still exposes it. */
    public static final class Uncopied {
        private final List<String> names;

        public Uncopied(List<String> names) {
            if (names != null) {
                names.size();
            }
            this.names = names;
        }

        public List<String> getNames() {
            return names;
        }
    }

    private Issue4033() {
    }
}
