import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Very small heuristic scanner that searches a SpotBugs source tree for classes
 * that look like detectors/visitors and prints a reStructuredText list-table
 * fragment to stdout. This is a helper for documentation authors, not a full
 * parser.
 *
 * Usage: java -cp . ExtractVisitors /path/to/spotbugs/spotbugs
 */
public class ExtractVisitors {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: ExtractVisitors <path-to-spotbugs-root>");
            System.exit(2);
        }
        Path root = Paths.get(args[0]);
        if (!Files.isDirectory(root)) {
            System.err.println("Not a directory: " + root);
            System.exit(2);
        }

        List<Path> javaFiles = Files.walk(root)
            .filter(p -> p.toString().endsWith(".java"))
            .collect(Collectors.toList());

        System.out.println(".. list-table:: SpotBugs visitors (auto-generated)");
        System.out.println("   :header-rows: 1\n");
        System.out.println("   * - Visitor class\n     - Description\n     - Bug patterns\n");

        for (Path p : javaFiles) {
            try {
                List<String> lines = Files.readAllLines(p);
                // join whole file content for pattern searches
                String content = String.join("\n", lines);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    // look for a class or interface declaration
                    if (line.matches(".*\\bclass\\b.*") || line.matches(".*\\binterface\\b.*")) {
                        // extract token after 'class' or 'interface'
                        String[] parts = line.split("\\bclass\\b|\\binterface\\b");
                        if (parts.length >= 2) {
                            String after = parts[1].trim();
                            String className = after.split("\\s+|\\{|\\(|;")[0];
                            // Heuristic: class name contains Detector or Visitor or Pattern or starts with Find
                            if (className.endsWith("Detector") || className.contains("Detector") || className.endsWith("Visitor") || className.contains("Visitor") || className.contains("Pattern") || className.startsWith("Find")) {
                                String rel = root.relativize(p).toString().replace('\\', '/');

                                // Try to extract a Javadoc comment immediately above the declaration
                                String description = "TBD";
                                for (int j = i - 1; j >= 0; j--) {
                                    String l = lines.get(j).trim();
                                    if (l.endsWith("*/")) {
                                        // find start of javadoc
                                        StringBuilder jd = new StringBuilder();
                                        for (int k = j; k >= 0; k--) {
                                            String lk = lines.get(k).trim();
                                            jd.insert(0, lk + "\n");
                                            if (lk.startsWith("/**")) {
                                                break;
                                            }
                                        }
                                        // clean javadoc: remove /**, */, leading * and take first paragraph
                                        String raw = jd.toString();
                                        raw = raw.replaceAll("(?m)^/\\*\\*\\s*", "");
                                        raw = raw.replaceAll("(?m)\\*/$", "");
                                        raw = raw.replaceAll("(?m)^\\s*\\*\\s?", "");
                                        String[] paras = raw.split("\\n\\s*\\n");
                                        if (paras.length > 0) {
                                            description = paras[0].trim().replaceAll("\\n", " ");
                                        } else {
                                            description = raw.trim().replaceAll("\\n", " ");
                                        }
                                        break;
                                    }
                                    if (!l.startsWith("*") && !l.isEmpty() && !l.startsWith("@")) {
                                        // non-javadoc content encountered; stop searching
                                        break;
                                    }
                                }

                                // Heuristic: find candidate bug-pattern-like identifiers (ALL_CAPS_WITH_UNDERSCORES)
                                Set<String> patterns = new HashSet<>();
                                Pattern pat = Pattern.compile("\\b[A-Z][A-Z0-9_]{5,}\\b");
                                Matcher m = pat.matcher(content);
                                while (m.find() && patterns.size() < 6) {
                                    String token = m.group();
                                    // skip obvious Java keywords or class names
                                    if (!token.equals(className) && !Character.isDigit(token.charAt(0))) {
                                        patterns.add(token);
                                    }
                                }
                                String bugPatterns = "TBD";
                                if (!patterns.isEmpty()) {
                                    bugPatterns = String.join(", ", patterns);
                                }

                                System.out.printf("   * - ``%s`` (in %s)\n     - %s\n     - %s\n\n", className, rel, escapeRst(description), escapeRst(bugPatterns));
                            }
                        }
                        break;
                    }
                }
            } catch (IOException ex) {
                // ignore files we can't read
            }
        }
    }

    private static String escapeRst(String s) {
        if (s == null) return "";
        // basic escaping: replace backticks, collapse whitespace
        String out = s.replace("`", "\\`");
        out = out.replaceAll("\\s+", " ").trim();
        // ensure the string is safe for inline table cells
        out = out.replaceAll("\r|\n", " ");
        return out;
    }
}
