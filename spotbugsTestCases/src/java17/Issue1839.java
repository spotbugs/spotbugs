import org.jetbrains.annotations.NotNull;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1839">GitHub Issue</a>
 */
public class Issue1839 {
    @NotNull
    String method(int major, int minor, int patchLevel, String suffix) {
        return "%d.%d.%d%s".formatted(major, minor, patchLevel, !suffix.isEmpty() ? "-"+suffix : "");
    }
}
