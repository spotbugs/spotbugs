package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317670">3.28 location object</a>
 */
class Location {
    @NonNull
    private final List<PhysicalLocation> physicalLocations;
    @NonNull
    private final List<LogicalLocation> logicalLocations;

    Location(PhysicalLocation physicalLocation, LogicalLocation logicalLocation) {
        if (physicalLocation == null && logicalLocation == null) {
            throw new IllegalArgumentException("One of physicalLocation or logicalLocation should be non-null");
        }

        physicalLocations = new ArrayList<>();
        if (physicalLocation != null) {
            physicalLocations.add(physicalLocation);
        }

        logicalLocations = new ArrayList<>();
        if (logicalLocation != null) {
            logicalLocations.add(logicalLocation);
        }
    }

    JSONObject toJsonObject() {
        JSONObject result = new JSONObject();
        physicalLocations.forEach(physicalLocation -> result.append("physicalLocations", physicalLocation.toJsonObject()));
        logicalLocations.forEach(logicalLocation -> result.append("logicalLocation", logicalLocation.toJsonObject()));
        return result;
    }

    static Optional<Location> fromBugInstance(@NonNull BugInstance bugInstance) {
        Objects.requireNonNull(bugInstance);

        final PhysicalLocation physicalLocation = findPhysicalLocation(bugInstance);
        return LogicalLocation.fromBugAnnotations(bugInstance.getAnnotations()).map(logicalLocation -> new Location(physicalLocation, logicalLocation));
    }

    @CheckForNull
    private static PhysicalLocation findPhysicalLocation(@NonNull BugInstance bugInstance) {
        try {
            SourceLineAnnotation sourceLine = bugInstance.getPrimarySourceLineAnnotation();
            return PhysicalLocation.fromBugAnnotation(sourceLine);
        } catch (IllegalStateException e) {
            // no sourceline info found
            return null;
        }
    }

    /**
     * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317427">3.4 artifactLocation object</a>
     */
    static final class ArtifactLocation {
        @NonNull
        final URI uri;
        @Nullable
        final String uriBaseId;

        ArtifactLocation(@NonNull URI uri, @Nullable String uriBaseId) {
            this.uri = Objects.requireNonNull(uri);
            this.uriBaseId = uriBaseId;
        }

        JSONObject toJsonObject() {
            return new JSONObject().put("uri", uri).putOpt("uriBaseId", uriBaseId);
        }

        static ArtifactLocation fromBugAnnotation(@NonNull SourceLineAnnotation bugAnnotation) {
            Objects.requireNonNull(bugAnnotation);
            // relative path from sourceRoot, see SourceFinder class for example
            String sourceFile = bugAnnotation.getSourceFile();
            // TODO support multiple srcRoot
            return new ArtifactLocation(URI.create(sourceFile), "srcRoot");
        }
    }

    /**
     * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317685">3.30 region object</a>
     */
    static final class Region {
        /**
         * 1-indexed line number
         */
        final int startLine;
        /**
         * 1-indexed line number
         */
        final int endLine;

        Region(int startLine, int endLine) {
            assert startLine > 0;
            assert endLine > 0;
            this.startLine = startLine;
            this.endLine = endLine;
        }

        static Region fromBugAnnotation(SourceLineAnnotation annotation) {
            return new Region(annotation.getStartLine(), annotation.getEndLine());
        }

        JSONObject toJsonObject() {
            return new JSONObject().put("startLine", startLine).put("endLine", endLine);
        }
    }

    /**
     * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317678">3.29 physicalLocation object</a>
     */
    static final class PhysicalLocation {
        @NonNull
        final ArtifactLocation artifactLocation;
        @Nullable
        final Region region;

        PhysicalLocation(@NonNull ArtifactLocation artifactLocation, @Nullable Region region) {
            this.artifactLocation = Objects.requireNonNull(artifactLocation);
            this.region = region;
        }

        JSONObject toJsonObject() {
            return new JSONObject().put("artifactLocation", artifactLocation.toJsonObject()).putOpt("region", region.toJsonObject());
        }

        static PhysicalLocation fromBugAnnotation(SourceLineAnnotation bugAnnotation) {
            ArtifactLocation artifactLocation = ArtifactLocation.fromBugAnnotation(bugAnnotation);
            Region region = Region.fromBugAnnotation(bugAnnotation);
            return new PhysicalLocation(artifactLocation, region);
        }
    }

    /**
     * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317719">3.33 logicalLocation object</a>
     */
    static final class LogicalLocation {
        @NonNull
        final String name;
        @NonNull
        final String decoratedName;
        @NonNull
        final String kind;
        // TODO resolve fullyQualifiedName parameter

        LogicalLocation(@NonNull String name, @NonNull String decoratedName, @NonNull String kind) {
            this.name = Objects.requireNonNull(name);
            this.decoratedName = Objects.requireNonNull(decoratedName);
            this.kind = Objects.requireNonNull(kind);
        }

        JSONObject toJsonObject() {
            return new JSONObject().put("name", name).put("decoratedName", decoratedName).put("kind", kind);
        }

        @NonNull
        static Optional<LogicalLocation> fromBugAnnotations(@NonNull Collection<? extends BugAnnotation> annotations) {
            Objects.requireNonNull(annotations);
            return annotations.stream().map(annotation -> {
                String kind = findKind(annotation);
                if (kind == null) {
                    return null;
                }
                String name = annotation.format("name", null);
                String decoratedName = annotation.format("register", null);
                return new LogicalLocation(name, decoratedName, kind);
            }).filter(Objects::nonNull).findFirst();
        }

        @CheckForNull
        static String findKind(@NonNull BugAnnotation annotation) {
            if (annotation instanceof ClassAnnotation) {
                return "type";
            } else if (annotation instanceof MethodAnnotation) {
                return "function";
            } else if (annotation instanceof FieldAnnotation) {
                return "member";
            } else if (annotation instanceof LocalVariableAnnotation) {
                return "variable";
            } else {
                return null;
            }
        }
    }
}
