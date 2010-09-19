package sfBugs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class Bug2893375 {
    public boolean noBug() {
        Field field = getField();
        return field.getType() == String.class && field.getAnnotation(Annotation.class) != null;
    }

    private Field getField() {
        return null;
    }
}
