package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.ExitCode;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONArray;
import org.json.JSONWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                    "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.4.json");
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
        processInvocations(jsonWriter, analyser.getBaseToId());
        jsonWriter.key("results").value(analyser.getResults());
        jsonWriter.key("originalUriBaseIds").value(analyser.getOriginalUriBaseIds());
        jsonWriter.endObject().endArray();
    }

    private void processInvocations(JSONWriter jsonWriter, @NonNull Map<String, String> baseToId) {
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

        ExitCode exitCode = ExitCode.from(getQueuedErrors().size(), missingClasses.size(), getBugCollection().getCollection().size());
        Invocation invocation = new Invocation(exitCode.getExitCode(),
                exitCode.getSignalName(), exitCode.getExitCode() == 0,
                execNotifications, configNotifications);
        jsonWriter.key("invocations").array().value(invocation.toJSONObject());
        jsonWriter.endArray();
    }

    private void processTool(@NonNull JSONWriter jsonWriter, @NonNull JSONArray rules) {
        jsonWriter.key("tool").object();
        processExtensions(jsonWriter);
        jsonWriter.key("driver").object();
        jsonWriter.key("name").value("SpotBugs");
        // Eclipse plugin does not follow the semantic-versioning, so use "version" instead of "semanticVersion".
        jsonWriter.key("version").value(Version.VERSION_STRING);
        // SpotBugs refers JVM config to decide which language we use.
        jsonWriter.key("language").value(Locale.getDefault().getLanguage());
        jsonWriter.key("rules").value(rules);
        jsonWriter.endObject().endObject();
    }

    private void processExtensions(@NonNull JSONWriter jsonWriter) {
        jsonWriter.key("extensions").array();
        DetectorFactoryCollection.instance().plugins().stream().map(Extension::fromPlugin).map(Extension::toJSONObject).forEach(jsonWriter::value);
        jsonWriter.endArray();
    }
}
