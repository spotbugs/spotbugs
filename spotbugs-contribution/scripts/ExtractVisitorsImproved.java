import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/**
 * ExtractVisitorsImproved - Generate visitor/detector documentation by parsing findbugs.xml
 * 
 * Addresses reviewer feedback:
 * - Uses findbugs.xml as the authoritative source for detectors
 * - Removes redundant pattern matching logic
 * - Provides more reliable detector discovery
 * 
 * Usage: java ExtractVisitorsImproved <spotbugs-source-root>
 */
public class ExtractVisitorsImproved {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java ExtractVisitorsImproved <spotbugs-source-root>");
            System.exit(1);
        }

        Path root = Paths.get(args[0]).toAbsolutePath();
        Path findbugsXml = root.resolve("spotbugs/etc/findbugs.xml");
        
        if (!Files.exists(findbugsXml)) {
            System.err.println("findbugs.xml not found at: " + findbugsXml);
            System.exit(1);
        }

        System.out.println("Parsing findbugs.xml from: " + findbugsXml);
        
        // Parse XML to extract detector classes
        Set<DetectorInfo> detectors = parseDetectorsFromXml(findbugsXml);
        
        // Find source files for each detector
        Map<String, String> classToSourceFile = buildClassToSourceMap(root);
        
        // Generate documentation
        generateDocumentation(detectors, classToSourceFile, root);
    }

    static class DetectorInfo {
        String className;
        String reports;
        String speed;
        boolean hidden;
        
        DetectorInfo(String className, String reports, String speed, boolean hidden) {
            this.className = className;
            this.reports = reports;
            this.speed = speed;
            this.hidden = hidden;
        }
    }

    private static Set<DetectorInfo> parseDetectorsFromXml(Path findbugsXml) throws Exception {
        Set<DetectorInfo> detectors = new LinkedHashSet<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(findbugsXml.toFile());
        
        NodeList detectorNodes = doc.getElementsByTagName("Detector");
        
        for (int i = 0; i < detectorNodes.getLength(); i++) {
            Element detector = (Element) detectorNodes.item(i);
            
            String className = detector.getAttribute("class");
            String reports = detector.getAttribute("reports");
            String speed = detector.getAttribute("speed");
            boolean hidden = "true".equals(detector.getAttribute("hidden"));
            
            if (!className.isEmpty()) {
                detectors.add(new DetectorInfo(className, reports, speed, hidden));
            }
        }
        
        System.out.println("Found " + detectors.size() + " detectors in findbugs.xml");
        return detectors;
    }

    private static Map<String, String> buildClassToSourceMap(Path root) throws IOException {
        Map<String, String> classToSource = new HashMap<>();
        
        Files.walk(root)
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(p -> {
                try {
                    String content = Files.readString(p);
                    Matcher matcher = Pattern.compile("(?:public\\s+)?(?:abstract\\s+)?class\\s+(\\w+)").matcher(content);
                    if (matcher.find()) {
                        String className = matcher.group(1);
                        classToSource.put(className, root.relativize(p).toString().replace('\\', '/'));
                    }
                } catch (IOException e) {
                    System.err.println("Error reading " + p + ": " + e.getMessage());
                }
            });
        
        return classToSource;
    }

    private static void generateDocumentation(Set<DetectorInfo> detectors, Map<String, String> classToSourceMap, Path root) {
        System.out.println("\n.. list-table:: SpotBugs Detectors and Visitors");
        System.out.println("   :header-rows: 1");
        System.out.println("   :widths: 30 20 15 15 20");
        System.out.println();
        System.out.println("   * - Detector Class");
        System.out.println("     - Bug Pattern IDs");  
        System.out.println("     - Speed");
        System.out.println("     - Visibility");
        System.out.println("     - Source File");

        for (DetectorInfo detector : detectors) {
            String shortClassName = detector.className.substring(detector.className.lastIndexOf('.') + 1);
            String sourceFile = classToSourceMap.get(shortClassName);
            
            if (sourceFile == null) {
                sourceFile = "Not found";
            }
            
            String visibility = detector.hidden ? "Hidden" : "Visible";
            String bugPatterns = detector.reports.isEmpty() ? "N/A" : detector.reports.replace(",", ", ");
            String speed = detector.speed.isEmpty() ? "N/A" : detector.speed;
            
            System.out.println("   * - ``" + escapeRst(shortClassName) + "``");
            System.out.println("     - " + escapeRst(bugPatterns));
            System.out.println("     - " + speed);
            System.out.println("     - " + visibility);
            System.out.println("     - " + escapeRst(sourceFile));
        }
        
        System.out.println();
        System.out.println("Generated documentation for " + detectors.size() + " detectors.");
        System.out.println("Non-hidden detectors: " + detectors.stream().mapToInt(d -> d.hidden ? 0 : 1).sum());
    }

    private static String escapeRst(String text) {
        if (text == null) return "";
        return text.replace("|", "\\|").replace("*", "\\*").replace("`", "\\`");
    }
}