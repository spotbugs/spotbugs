package sfBugsNew;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Feature331 {
    @ExpectWarning("PT_ABSOLUTE_PATH_TRAVERSAL")
    public String getFileContent(HttpServletRequest req) throws IOException {
        return new String(readFile(req.getParameter("path")), StandardCharsets.UTF_8);
    }

    private byte[] readFile(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(fileName));
    }
}
