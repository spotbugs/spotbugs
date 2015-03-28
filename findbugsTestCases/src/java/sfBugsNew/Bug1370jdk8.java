package sfBugsNew;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1370jdk8 {
    public List<Path> readLocations(final List<String> inputs) {
        return inputs.stream()
                .map(Bug1370jdk8::createPath).collect(Collectors.toList());
    }

    @NoWarning("UPM_UNCALLED_PRIVATE_METHOD")
    private static Path createPath(final String input) {
        // doesn't matter
        return null;
    }
}
