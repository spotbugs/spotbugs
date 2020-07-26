package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONArray;
import org.json.JSONWriter;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SarifBugReporter extends BugCollectionBugReporter {
    public SarifBugReporter(Project project) {
        super(project);
    }

    @Override
    public void finish() {
        try {
            JSONWriter jsonWriter = new JSONWriter(outputStream);
            jsonWriter.object();
            jsonWriter.key("version").value("2.1.0").key("$schema").value(
                    "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json");
            processRuns(jsonWriter);
            jsonWriter.endObject();
            getBugCollection().bugsPopulated();
        } finally {
            outputStream.close();
        }
    }

    private void processRuns(@NonNull JSONWriter jsonWriter) {
        jsonWriter.key("runs").array().object();
        BugCollectionAnalyser analyser = new BugCollectionAnalyser(getBugCollection());
        processTool(jsonWriter, analyser.getRules(), analyser.getBaseToId());
        jsonWriter.key("results").value(analyser.getResults());
        jsonWriter.key("originalUriBaseIds").value(analyser.getOriginalUriBaseIds());
        jsonWriter.endObject().endArray();
    }

    private void processTool(@NonNull JSONWriter jsonWriter, @NonNull JSONArray rules, @NonNull Map<String, String> baseToId) {
        jsonWriter.key("tool").object();
        processExtensions(jsonWriter);
        jsonWriter.key("driver").object();
        jsonWriter.key("name").value("SpotBugs");
        // Eclipse plugin does not follow the semantic-versioning, so use "version" instead of "semanticVersion".
        jsonWriter.key("version").value(Version.VERSION_STRING);
        // SpotBugs refers JVM config to decide which language we use.
        jsonWriter.key("language").value(Locale.getDefault().getLanguage());
        jsonWriter.key("rules").value(rules);
        processNotifications(jsonWriter, baseToId);
        jsonWriter.endObject().endObject();
    }

    private void processExtensions(@NonNull JSONWriter jsonWriter) {
        jsonWriter.key("extensions").array();
        DetectorFactoryCollection.instance().plugins().stream().map(Extension::fromPlugin).map(Extension::toJSONObject).forEach(jsonWriter::value);
        jsonWriter.endArray();
    }

    private void processNotifications(JSONWriter jsonWriter, @NonNull Map<String, String> baseToId) {
        jsonWriter.key("notifications").array();
        Set<String> missingClasses = getMissingClasses();
        if (missingClasses != null && !missingClasses.isEmpty()) {
            String message = String.format("Classes needed for analysis were missing: %s", missingClasses.toString());
            Notification notification = new Notification("spotbugs-missing-classes", message, Level.ERROR, null);
            jsonWriter.value(notification.toJSONObject());
        }
        getQueuedErrors().stream().map(t -> Notification.fromError(t, getProject().getSourceFinder(), baseToId)).map(Notification::toJSONObject)
                .forEach(jsonWriter::value);
        jsonWriter.endArray();
    }
}
