package ghIssues;

public class Issue4305 {
    public Long convert(Object value) {
        assert value instanceof Long;
        return (Long) value;
    }

    public void assertReceiver() {
        assert this instanceof Runnable;
    }

    public static Long convertStatic(Object value) {
        assert value instanceof Long;
        return (Long) value;
    }

    public Object convertOther(Object value) {
        CharSequence other = "x";
        assert other instanceof String;
        return value;
    }

    private Long privateConvert(Object value) {
        assert value instanceof Long;
        return (Long) value;
    }
}

class Issue4305Outer {
    public static class Subject {
        public Long convert(Object value) {
            assert value instanceof Long;
            return (Long) value;
        }
    }
}
