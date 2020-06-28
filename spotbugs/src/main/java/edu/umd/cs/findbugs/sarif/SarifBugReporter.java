package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONArray;
import org.json.JSONWriter;

import java.io.PrintWriter;
import java.util.Locale;

public class SarifBugReporter extends BugCollectionBugReporter {
    public SarifBugReporter(Project project) {
        super(project);
    }

    @Override
    public void finish() {
        JSONWriter jsonWriter = new JSONWriter(outputStream);
        jsonWriter.object();
        jsonWriter.key("version").value("2.1.0").key("$schema").value("https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.4.json");
        processRuns(jsonWriter);
        jsonWriter.endObject();
        super.finish();
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
        // TODO process "notifications"
        jsonWriter.endObject().endObject();
    }
}