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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317670">3.28 location object</a>
 */
class Location {
    @Nullable
    private final PhysicalLocation physicalLocation;
    @NonNull
    private final List<LogicalLocation> logicalLocations;

    Location(@Nullable PhysicalLocation physicalLocation, @NonNull Collection<LogicalLocation> logicalLocations) {
        if (physicalLocation == null && (Objects.requireNonNull(logicalLocations).isEmpty())) {
            throw new IllegalArgumentException("One of physicalLocation or logicalLocations should exist");
        }

        this.physicalLocation = physicalLocation;
        this.logicalLocations = new ArrayList<>(logicalLocations);
    }

    JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        if (physicalLocation != null) {
            result.put("physicalLocation", physicalLocation.toJSONObject());
        }
        logicalLocations.stream().map(LogicalLocation::toJSONObject).forEach(logicalLocation -> result.append("logicalLocation", logicalLocation));
        return result;
    }

    static Optional<Location> fromBugInstance(@NonNull BugInstance bugInstance) {
        Objects.requireNonNull(bugInstance);

        final PhysicalLocation physicalLocation = findPhysicalLocation(bugInstance);
        return LogicalLocation.fromBugInstance(bugInstance).map(logicalLocation -> new Location(physicalLocation,
                Collections.singleton(logicalLocation)));
    }

    @CheckForNull
    private static PhysicalLocation findPhysicalLocation(@NonNull BugInstance bugInstance) {
        try {
            SourceLineAnnotation sourceLine = bugInstance.getPrimarySourceLineAnnotation();
            return PhysicalLocation.fromBugAnnotation(sourceLine).orElse(null);
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

        JSONObject toJSONObject() {
            return new JSONObject().put("uri", uri).putOpt("uriBaseId", uriBaseId);
        }

        static Optional<ArtifactLocation> fromBugAnnotation(@NonNull SourceLineAnnotation bugAnnotation) {
            Objects.requireNonNull(bugAnnotation);
            // relative path from sourceRoot, see SourceFinder class for example
            String sourceFile = bugAnnotation.getSourceFile();
            // TODO support multiple srcRoot
            try {
                return Optional.of(new ArtifactLocation(new URI(sourceFile), "srcRoot"));
            } catch (URISyntaxException e) {
                return Optional.empty();
            }
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

        static Optional<Region> fromBugAnnotation(SourceLineAnnotation annotation) {
            if (annotation.getStartLine() <= 0 || annotation.getEndLine() <= 0) {
                return Optional.empty();
            } else {
                return Optional.of(new Region(annotation.getStartLine(), annotation.getEndLine()));
            }
        }

        JSONObject toJSONObject() {
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

        JSONObject toJSONObject() {
            return new JSONObject().put("artifactLocation", artifactLocation.toJSONObject()).putOpt("region", region.toJSONObject());
        }

        static Optional<PhysicalLocation> fromBugAnnotation(SourceLineAnnotation bugAnnotation) {
            Optional<ArtifactLocation> artifactLocation = ArtifactLocation.fromBugAnnotation(bugAnnotation);
            Optional<Region> region = Region.fromBugAnnotation(bugAnnotation);
            return artifactLocation.map(location -> new PhysicalLocation(location, region.orElse(null)));
        }
    }

    /**
     * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317719">3.33 logicalLocation object</a>
     */
    static final class LogicalLocation {
        @NonNull
        final String name;
        @Nullable
        final String decoratedName;
        @NonNull
        final String kind;
        // TODO resolve fullyQualifiedName parameter

        LogicalLocation(@NonNull String name, @Nullable String decoratedName, @NonNull String kind) {
            this.name = Objects.requireNonNull(name);
            this.decoratedName = decoratedName;
            this.kind = Objects.requireNonNull(kind);
        }

        JSONObject toJSONObject() {
            return new JSONObject().put("name", name).putOpt("decoratedName", decoratedName).put("kind", kind);
        }

        @NonNull
        static Optional<LogicalLocation> fromBugInstance(@NonNull BugInstance bugInstance) {
            Objects.requireNonNull(bugInstance);
            ClassAnnotation primaryClass = bugInstance.getPrimaryClass();
            return bugInstance.getAnnotations().stream().map(annotation -> {
                String kind = findKind(annotation);
                if (kind == null) {
                    return null;
                }
                String name = annotation.format("givenClass", primaryClass);
                // TODO how to find decoratedName
                return new LogicalLocation(name, null, kind);
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
