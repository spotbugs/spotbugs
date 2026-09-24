package ghIssues;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Issue3830 {

    private final int gridSize;
    private final Set<Pair<Integer, Integer>> set = new HashSet<>();

    public Issue3830(final int gridSize) {
        this.gridSize = gridSize;
        final Random random = new Random();
        while (set.size() < 3) {
            // Random is used multiple times here
            this.set.add(new Pair<>(
                    random.nextInt(gridSize), // reported here
                    random.nextInt(gridSize)));
        }
    }

    private record Pair<A, B>(A first, B second) {}
}
