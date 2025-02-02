package edu.umd.cs.findbugs.sarif;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.ExitCodes;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.cwe.WeaknessCatalog;

public class SarifBugReporter extends BugCollectionBugReporter {
    private final Gson gson = new Gson();

    public SarifBugReporter(Project project) {
        super(project);
    }

    @Override
    public void finish() {
        try {
            JsonWriter jsonWriter = new JsonWriter(outputStream);
            jsonWriter.beginObject();
            jsonWriter.name("version").value("2.1.0").name("$schema").value(
                    "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json");
            processRuns(jsonWriter);
            jsonWriter.endObject();
            getBugCollection().bugsPopulated();
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while exporting to Sarif Json log.", e);
        } finally {
            outputStream.close();
        }
    }

    private void processRuns(@NonNull JsonWriter jsonWriter) throws IOException {

        jsonWriter.name("runs").beginArray().beginObject();
        BugCollectionAnalyser analyser = new BugCollectionAnalyser(getBugCollection());
        processTool(jsonWriter, analyser.getRules());
        processInvocations(jsonWriter, analyser.getBaseToId());

        jsonWriter.name("results").beginArray();
        analyser.getResults().forEach((result) -> gson.toJson(result, jsonWriter));
        jsonWriter.endArray();

        jsonWriter.name("originalUriBaseIds");
        gson.toJson(analyser.getOriginalUriBaseIds(), jsonWriter);

        jsonWriter.name("taxonomies").beginArray();
        JsonObject taxa = analyser.getCweTaxonomy();
        if (taxa != null && !taxa.isJsonNull()) {
            gson.toJson(taxa, jsonWriter);
        }
        jsonWriter.endArray();

        jsonWriter.endObject().endArray(); // end "runs", end "runs" array
    }

    private void processInvocations(JsonWriter jsonWriter, @NonNull Map<URI, String> baseToId) throws IOException {
        List<Notification> configNotifications = new ArrayList<>();

        Set<String> missingClasses = getMissingClasses();
        if (missingClasses == null) {
            missingClasses = Collections.emptySet();
        }
        if (!missingClasses.isEmpty()) {
            String message = String.format("Classes needed for analysis were missing: %s", missingClasses.toString());
            configNotifications.add(new Notification("spotbugs-missing-classes", message, Level.ERROR, null));
        }

        List<Notification> execNotifications = getQueuedErrors().stream()
                .map(t -> Notification.fromError(t, getProject().getSourceFinder(), baseToId))
                .collect(Collectors.toList());

        int exitCode = ExitCodes.from(getQueuedErrors().size(), missingClasses.size(), getBugCollection().getCollection().size());
        Invocation invocation = new Invocation(exitCode,
                getExitCodeDescription(exitCode),
                (exitCode | ExitCodes.BUGS_FOUND_FLAG) == ExitCodes.BUGS_FOUND_FLAG,
                execNotifications,
                configNotifications);

        jsonWriter.name("invocations").beginArray();
        gson.toJson(invocation.toJsonObject(), jsonWriter);
        jsonWriter.endArray();
    }

    private void processTool(@NonNull JsonWriter jsonWriter, @NonNull JsonArray rules) throws IOException {
        jsonWriter.name("tool").beginObject();
        processExtensions(jsonWriter);

        jsonWriter.name("driver").beginObject();
        jsonWriter.name("name").value("SpotBugs");
        // Eclipse plugin does not follow the semantic-versioning, so use "version" instead of "semanticVersion".
        jsonWriter.name("version").value(Version.VERSION_STRING);
        // SpotBugs refers JVM config to decide which language we use.
        jsonWriter.name("language").value(Locale.getDefault().getLanguage());
        jsonWriter.name("informationUri").value("https://spotbugs.github.io/");

        jsonWriter.name("rules").beginArray();
        rules.forEach((rule) -> gson.toJson(rule, jsonWriter));
        jsonWriter.endArray();

        addSupportedTaxonomies(jsonWriter);

        jsonWriter.endObject().endObject(); // end "driver", end "tool"
    }

    private void processExtensions(@NonNull JsonWriter jsonWriter) throws IOException {

        jsonWriter.name("extensions").beginArray();
        DetectorFactoryCollection.instance().plugins().stream().map(Extension::fromPlugin).map(Extension::toJsonObject).forEach((
                jsonObject) -> gson.toJson(jsonObject, jsonWriter));
        jsonWriter.endArray();
    }

    private void addSupportedTaxonomies(@NonNull JsonWriter jsonWriter) throws IOException {
        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        String name = weaknessCatalog.getName();
        String version = weaknessCatalog.getVersion();

        UUID uuid = GUIDCalculator.fromString(name + version);


        jsonWriter.name("supportedTaxonomies").beginArray();

        jsonWriter.beginObject();
        jsonWriter.name("name").value(name);

        jsonWriter.name("guid").value(uuid.toString());

        jsonWriter.endObject();
        jsonWriter.endArray();
    }

    private static String getExitCodeDescription(int exitCode) {
        if (exitCode == 0) {
            return "SUCCESS";
        }

        List<String> list = new ArrayList<>();
        if ((exitCode & ExitCodes.ERROR_FLAG) > 0) {
            list.add("ERROR");
        }
        if ((exitCode & ExitCodes.MISSING_CLASS_FLAG) > 0) {
            list.add("MISSING CLASS");
        }
        if ((exitCode & ExitCodes.BUGS_FOUND_FLAG) > 0) {
            list.add("BUGS FOUND");
        }

        return list.isEmpty() ? "UNKNOWN" : String.join(",", list);
    }
}
