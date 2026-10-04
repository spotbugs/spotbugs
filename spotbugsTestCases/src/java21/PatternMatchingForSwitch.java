class Shape {
}

class Rectangle extends Shape {
}

class Triangle extends Shape {
    int calculateArea() {
        return 0;
    }
}

/**
 * @see <a href="https://openjdk.org/jeps/441">JEP 441: Pattern Matching for switch</a>
 */
public class PatternMatchingForSwitch {
    String formatterPatternSwitch(Object o) {
        return switch (o) {
            case Integer i -> String.format("int %d", i);
            case Long l -> String.format("long %d", l);
            case Double d -> String.format("double %f", d);
            case String s -> String.format("String %s", s);
            case null -> "null";
            default -> o.toString();
        };
    }

    void testTriangle(Shape s) {
        switch (s) {
            case Triangle t when t.calculateArea() > 100 ->
                System.out.println("Large triangle");
            case Triangle t ->
                System.out.println("Small triangle");
            default -> System.out.println("Non-triangle");
        }
    }
}
