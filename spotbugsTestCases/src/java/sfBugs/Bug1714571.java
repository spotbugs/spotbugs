package sfBugs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Bug1714571 {
    void f(Object doc, File docFile) throws IOException {
        Map<Integer, String> attachments = new HashMap<Integer, String>();

        if ((docFile != null) && docFile.exists()) {
            attachments.put(doc.hashCode(), doc.toString());
        } else {
            System.out.println("Can not read uploaded file: " + ((docFile != null) ? docFile.getAbsolutePath() : "") + " : "
                    + doc.toString());
            throw new FileNotFoundException("file not found in disk!" + ((docFile != null) ? docFile.getAbsolutePath() : "")
                    + " : " + doc.toString());
        }
    }

}
