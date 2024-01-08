package lambdas;
import java.util.List;
public class Issue547 {
    static boolean doSomething(List<String> dups) {
        return dups.stream().anyMatch(x -> x.isEmpty());
    }
}
