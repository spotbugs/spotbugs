package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.Project;
import org.json.JSONWriter;

import javax.annotation.CheckForNull;
import java.io.PrintWriter;

public class SarifBugReporter extends BugCollectionBugReporter {
    public SarifBugReporter(Project project) {
        super(project);
    }

    @Override
    public void finish() {
        try (PrintWriter writer = outputStream) {
            JSONWriter jsonWriter = new JSONWriter(writer);
            jsonWriter.object().key("version").value("2.1.0").key("$schema").value("https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.4.json").endObject();
        }
    }
}