// https://github.com/spotbugs/spotbugs/issues/3196
public class Issue3196 {
    public record One(String value) {
    }

    public record Two(One one) {
    }

    public record Foo(String bar, int x, String baz) {
    }

    public record Holder(Foo foo) {
    }

    // javac tests the result of two.one() with an instanceof that can't fail
    public String nestedSwitch(Object obj) {
        return switch (obj) {
            case One(var value) -> value;
            case Two(One(var value)) -> value;
            case null, default -> null;
        };
    }

    // foo is already a Foo, so the pattern's instanceof only rejects null
    public String topLevel(Foo foo) {
        if (foo instanceof Foo(var bar, var x, var baz)) {
            return bar + x + baz;
        }
        return null;
    }

    public String nestedIf(Holder holder) {
        if (holder instanceof Holder(Foo(var bar, var x, var baz))) {
            return bar + x + baz;
        }
        return null;
    }

    // The same checks written by hand are still vacuous
    public boolean handWrittenNested(Two two) {
        One one = two.one();
        return one instanceof One;
    }

    public boolean handWrittenTopLevel(Foo foo) {
        return foo instanceof Foo;
    }
}
