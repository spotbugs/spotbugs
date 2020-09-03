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
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.util.ClassName;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @CheckForNull
    PhysicalLocation getPhysicalLocation() {
        return physicalLocation;
    }

    JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        if (physicalLocation != null) {
            result.put("physicalLocation", physicalLocation.toJSONObject());
        }
        logicalLocations.stream().map(LogicalLocation::toJSONObject).forEach(logicalLocation -> result.append("logicalLocations", logicalLocation));
        return result;
    }

    static Optional<Location> fromBugInstance(@NonNull BugInstance bugInstance, @NonNull SourceFinder sourceFinder,
            @NonNull Map<URI, String> baseToId) {
        Objects.requireNonNull(bugInstance);
        Objects.requireNonNull(sourceFinder);
        Objects.requireNonNull(baseToId);

        final PhysicalLocation physicalLocation = findPhysicalLocation(bugInstance, sourceFinder, baseToId);
        return LogicalLocation.fromBugInstance(bugInstance).map(logicalLocation -> new Location(physicalLocation,
                Collections.singleton(logicalLocation)));
    }

    static Location fromStackTraceElement(@NonNull StackTraceElement element, @NonNull SourceFinder sourceFinder,
            @NonNull Map<URI, String> baseToId) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(sourceFinder);
        Objects.requireNonNull(baseToId);

        Optional<PhysicalLocation> physicalLocation = findPhysicalLocation(element, sourceFinder, baseToId);
        LogicalLocation logicalLocation = LogicalLocation.fromStackTraceElement(element);
        return new Location(physicalLocation.orElse(null), Collections.singleton(logicalLocation));
    }

    @CheckForNull
    private static PhysicalLocation findPhysicalLocation(@NonNull BugInstance bugInstance, @NonNull SourceFinder sourceFinder,
            Map<URI, String> baseToId) {
        try {
            SourceLineAnnotation sourceLine = bugInstance.getPrimarySourceLineAnnotation();
            return PhysicalLocation.fromBugAnnotation(sourceLine, sourceFinder, baseToId).orElse(null);
        } catch (IllegalStateException e) {
            // no sourceline info found
            return null;
        }
    }

    @CheckForNull
    private static Optional<PhysicalLocation> findPhysicalLocation(@NonNull StackTraceElement element, @NonNull SourceFinder sourceFinder,
            Map<URI, String> baseToId) {
        Optional<Region> region = Optional.of(element.getLineNumber())
                .filter(line -> line > 0)
                .map(line -> new Region(line, line));
        return ArtifactLocation.fromStackTraceElement(element, sourceFinder, baseToId)
                .map(artifactLocation -> new PhysicalLocation(artifactLocation, region.orElse(null)));
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

        static Optional<ArtifactLocation> fromBugAnnotation(@NonNull SourceLineAnnotation bugAnnotation, @NonNull SourceFinder sourceFinder,
                @NonNull Map<URI, String> baseToId) {
            Objects.requireNonNull(bugAnnotation);
            Objects.requireNonNull(sourceFinder);
            Objects.requireNonNull(baseToId);

            return sourceFinder.getBase(bugAnnotation).map(base -> {
                String uriBaseId = baseToId.computeIfAbsent(base, s -> Integer.toString(s.hashCode()));
                try {
                    SourceFile sourceFile = sourceFinder.findSourceFile(bugAnnotation);
                    return new ArtifactLocation(base.relativize(sourceFile.getFullURI()), uriBaseId);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        static Optional<ArtifactLocation> fromStackTraceElement(StackTraceElement element, SourceFinder sourceFinder, Map<URI, String> baseToId) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(sourceFinder);
            Objects.requireNonNull(baseToId);

            String packageName = ClassName.extractPackageName(element.getClassName());
            String fileName = element.getFileName();
            try {
                SourceFile sourceFile = sourceFinder.findSourceFile(packageName, fileName);
                String fullFileName = sourceFile.getFullFileName();
                int index = fullFileName.indexOf(packageName.replace('.', File.separatorChar));
                assert index >= 0;
                String relativeFileName = fullFileName.substring(index);
                return sourceFinder.getBase(relativeFileName).map(base -> {
                    String baseId = baseToId.computeIfAbsent(base, s -> Integer.toString(s.hashCode()));
                    URI relativeUri = base.relativize(sourceFile.getFullURI());
                    return new ArtifactLocation(relativeUri, baseId);
                });
            } catch (IOException fileNotFound) {
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
            JSONObject result = new JSONObject().put("artifactLocation", artifactLocation.toJSONObject());
            if (region != null) {
                result.put("region", region.toJSONObject());
            }
            return result;
        }

        static Optional<PhysicalLocation> fromBugAnnotation(SourceLineAnnotation bugAnnotation, SourceFinder sourceFinder,
                Map<URI, String> baseToId) {
            Optional<ArtifactLocation> artifactLocation = ArtifactLocation.fromBugAnnotation(bugAnnotation, sourceFinder, baseToId);
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
        @Nullable
        final String fullyQualifiedName;
        @Nullable
        final Map<String, String> properties = new HashMap<>();

        LogicalLocation(@NonNull String name, @Nullable String decoratedName, @NonNull String kind, @Nullable String fullyQualifiedName,
                @Nullable Map<String, String> properties) {
            this.name = Objects.requireNonNull(name);
            this.decoratedName = decoratedName;
            this.kind = Objects.requireNonNull(kind);
            this.fullyQualifiedName = fullyQualifiedName;
            if (properties != null) {
                this.properties.putAll(properties);
            }
        }

        JSONObject toJSONObject() {
            JSONObject propertiesBag = new JSONObject(properties);
            return new JSONObject().put("name", name).putOpt("decoratedName", decoratedName).put("kind", kind).putOpt("fullyQualifiedName",
                    fullyQualifiedName).putOpt("properties", propertiesBag.isEmpty() ? null : propertiesBag);
        }

        @NonNull
        static LogicalLocation fromStackTraceElement(@NonNull StackTraceElement element) {
            String fullyQualifiedName = String.format("%s.%s", element.getClassName(), element.getMethodName());
            Map<String, String> properties = new HashMap<>();
            properties.put("line-number", Integer.toString(element.getLineNumber()));
            return new LogicalLocation(element.getMethodName(), null, "function", fullyQualifiedName, properties);
        }

        @NonNull
        static Optional<LogicalLocation> fromBugInstance(@NonNull BugInstance bugInstance) {
            Objects.requireNonNull(bugInstance);
            ClassAnnotation primaryClass = bugInstance.getPrimaryClass();
            SourceLineAnnotation sourceLine = bugInstance.getPrimarySourceLineAnnotation();
            return bugInstance.getAnnotations().stream().map(annotation -> {
                String kind = findKind(annotation);
                if (kind == null) {
                    return null;
                }
                String name = annotation.format("givenClass", primaryClass);
                return new LogicalLocation(name, null, kind, sourceLine.format("full", primaryClass), null);
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
