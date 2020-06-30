package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONArray;
import org.json.JSONWriter;

import java.util.Locale;
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
            jsonWriter.key("version").value("2.1.0").key("$schema").value("https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.4.json");
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
        processTool(jsonWriter, analyser.getRules());
        jsonWriter.key("results").value(analyser.getResults());
        jsonWriter.endObject().endArray();
    }

    private void processTool(@NonNull JSONWriter jsonWriter, @NonNull JSONArray rules) {
        jsonWriter.key("tool").object().key("driver").object();
        jsonWriter.key("name").value(Version.getApplicationName());
        // Eclipse plugin does not follow the semantic-versioning, so use "version" instead of "semanticVersion".
        jsonWriter.key("version").value(Version.getApplicationVersion());
        // SpotBugs refers JVM config to decide which language we use.
        jsonWriter.key("language").value(Locale.getDefault().getLanguage());
        jsonWriter.key("rules").value(rules);
        processNotifications(jsonWriter);
        jsonWriter.endObject().endObject();
    }

    private void processNotifications(JSONWriter jsonWriter) {
        jsonWriter.key("notifications").array();
        Set<String> missingClasses = getMissingClasses();
        if (missingClasses != null && !missingClasses.isEmpty()) {
            String message = String.format("Classes needed for analysis were missing: %s", missingClasses.toString());
            Notification notification = new Notification("spotbugs-missing-classes", message, Level.ERROR, null);
            jsonWriter.value(notification.toJsonObject());
        }
        getQueuedErrors().stream().map(Notification::fromError).map(Notification::toJsonObject).forEach(jsonWriter::value);
        jsonWriter.endArray();
    }
}
