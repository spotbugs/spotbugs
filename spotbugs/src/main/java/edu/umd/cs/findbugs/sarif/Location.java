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
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
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

    JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        if (physicalLocation != null) {
            result.add("physicalLocation", physicalLocation.toJsonObject());
        }

        JsonArray logicalLocationArray = new JsonArray();
        logicalLocations.stream().map(LogicalLocation::toJsonObject).forEach(logicalLocation -> logicalLocationArray.add(logicalLocation));
        if (logicalLocationArray.size() > 0) {
            result.add("logicalLocations", logicalLocationArray);
        }
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
            return PhysicalLocation.fromBugAnnotation(bugInstance, sourceFinder, baseToId).orElse(null);
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

        JsonObject toJsonObject() {
            JsonObject locationJson = new JsonObject();
            locationJson.addProperty("uri", uri.toString());
            locationJson.addProperty("uriBaseId", uriBaseId);
            return locationJson;
        }

        static Optional<ArtifactLocation> fromBugAnnotation(@NonNull ClassAnnotation classAnnotation, @NonNull SourceLineAnnotation bugAnnotation,
                @NonNull SourceFinder sourceFinder,
                @NonNull Map<URI, String> baseToId) {
            Objects.requireNonNull(bugAnnotation);
            Objects.requireNonNull(sourceFinder);
            Objects.requireNonNull(baseToId);

            Optional<ArtifactLocation> location = sourceFinder.getBase(bugAnnotation).map(base -> {
                String uriBaseId = baseToId.computeIfAbsent(base, s -> Integer.toString(s.hashCode()));
                try {
                    SourceFile sourceFile = sourceFinder.findSourceFile(bugAnnotation);
                    return new ArtifactLocation(base.relativize(sourceFile.getFullURI()), uriBaseId);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            if (location.isPresent()) {
                return location;
            }

            try {
                String path = bugAnnotation.format("full", classAnnotation);
                String pathWithoutLine = path.contains(":") ? path.split(":")[0] : path;
                return Optional.of(new ArtifactLocation(new URI(pathWithoutLine), null));
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return Optional.empty();
            }
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
            this.endLine = startLine == endLine ? 0 : endLine;
        }

        static Optional<Region> fromBugAnnotation(SourceLineAnnotation annotation) {
            if (annotation.getStartLine() <= 0 || annotation.getEndLine() <= 0) {
                return Optional.empty();
            } else {
                return Optional.of(new Region(annotation.getStartLine(), annotation.getEndLine()));
            }
        }

        JsonObject toJsonObject() {
            JsonObject json = new JsonObject();
            json.addProperty("startLine", startLine);

            if (endLine != 0) {
                json.addProperty("endLine", endLine);
            }

            return json;
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

        JsonObject toJsonObject() {
            JsonObject result = new JsonObject();
            result.add("artifactLocation", artifactLocation.toJsonObject());
            if (region != null) {
                result.add("region", region.toJsonObject());
            }
            return result;
        }

        static Optional<PhysicalLocation> fromBugAnnotation(@NonNull BugInstance bugInstance, SourceFinder sourceFinder,
                Map<URI, String> baseToId) {
            ClassAnnotation primaryClass = bugInstance.getPrimaryClass();
            SourceLineAnnotation sourceLine = bugInstance.getPrimarySourceLineAnnotation();

            Optional<ArtifactLocation> artifactLocation = ArtifactLocation.fromBugAnnotation(primaryClass, sourceLine, sourceFinder, baseToId);
            Optional<Region> region = Region.fromBugAnnotation(sourceLine);
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
        @NonNull
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

        JsonObject toJsonObject() {
            JsonObject propertiesBag = new JsonObject();
            properties.forEach((k, v) -> propertiesBag.addProperty(k, v));
            JsonObject locationJson = new JsonObject();
            locationJson.addProperty("name", name);
            if (decoratedName != null) {
                locationJson.addProperty("decoratedName", decoratedName);
            }
            locationJson.addProperty("kind", kind);
            if (fullyQualifiedName != null) {
                locationJson.addProperty("fullyQualifiedName", fullyQualifiedName);
            }
            if (propertiesBag != null && propertiesBag.size() > 0) {
                locationJson.add("properties", propertiesBag);
            }
            return locationJson;
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
            ClassAnnotation classAnnotation = null;
            MethodAnnotation methodAnnotation = null;
            FieldAnnotation fieldAnnotation = null;
            LocalVariableAnnotation localVariableAnnotation = null;

            for (BugAnnotation bugAnnotation : bugInstance.getAnnotations()) {
                if (bugAnnotation instanceof ClassAnnotation) {
                    classAnnotation = (ClassAnnotation) bugAnnotation;
                } else if (bugAnnotation instanceof MethodAnnotation) {
                    methodAnnotation = (MethodAnnotation) bugAnnotation;
                } else if (bugAnnotation instanceof FieldAnnotation) {
                    fieldAnnotation = (FieldAnnotation) bugAnnotation;
                } else if (bugAnnotation instanceof LocalVariableAnnotation) {
                    localVariableAnnotation = (LocalVariableAnnotation) bugAnnotation;
                }
            }

            String fullyQualifiedName = "";
            BugAnnotation annotation = null;
            if (localVariableAnnotation != null) {
                annotation = localVariableAnnotation;
                fullyQualifiedName = String.format("%s#%s", methodAnnotation.getFullMethod(classAnnotation), localVariableAnnotation.getName());
            } else {
                if (fieldAnnotation != null) {
                    annotation = fieldAnnotation;
                    fullyQualifiedName = String.format("%s.%s", classAnnotation.getClassName(), fieldAnnotation.getFieldName());
                } else {
                    if (methodAnnotation != null) {
                        annotation = methodAnnotation;
                        fullyQualifiedName = methodAnnotation.getFullMethod(classAnnotation);
                    } else if (classAnnotation != null) {
                        annotation = classAnnotation;
                        fullyQualifiedName = classAnnotation.getClassName();
                    }
                }
            }

            if (annotation == null) {
                return Optional.empty();
            }

            String kind = findKind(annotation);
            String name = annotation.format("givenClass", classAnnotation);
            return Optional.of(new LogicalLocation(name, null, kind, fullyQualifiedName, null));
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
