package constructorthrow;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;

/**
 * Based on <a href="https://github.com/spotbugs/spotbugs/issues/3821">GitHub issue #3821</a>
 *
 */
public class ConstructorThrowTest25 {
    public ConstructorThrowTest25(Object param) {
        Objects.requireNonNull(param);
    }

    public ConstructorThrowTest25(List<String> list, int idx) {
        Validate.validIndex(list, idx);
    }

    public ConstructorThrowTest25(List<String> list, int from, int to) {
        Objects.checkFromIndexSize(from, to, list.size());
    }

    public ConstructorThrowTest25(String str) {
        try {
            Preconditions.checkNotNull(str);
            Preconditions.checkArgument(!str.isBlank());
        } catch (NullPointerException e) {
            // this does not catch the IllegalArgumentException thrown by checkArgument,
            // so there is still a bug
        }
    }
}
