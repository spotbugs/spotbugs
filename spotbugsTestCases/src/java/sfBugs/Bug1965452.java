package sfBugs;

import java.io.File;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * 
 * @version $Id: NonNullFalsePositive.java,v 1.9 2008/05/16 15:35:01 ped Exp $
 */
public class Bug1965452 {

    /**
     * @author Carsten Heyl
     * @version $Id: ClientFileImportObject.java,v 1.3 2008/05/16 15:24:25 cal
     *          Exp $
     */
    @DefaultAnnotation(NonNull.class)
    public static class ClientFileImportObject {
        @CheckForNull
        private final String attributes;

        public ClientFileImportObject(File existingFile, @Nullable String mimeType, String attributes2) {
            this.attributes = attributes2;
        }
    }

    public static class ImportData {
        final ClientFileImportObject importObject;

        final File existingFile;

        public ImportData(ClientFileImportObject importObject, File existingFile) {
            this.importObject = importObject;
            this.existingFile = existingFile;
        }

        public ClientFileImportObject getImportObject() {
            return importObject;
        }
    }

    public void doImport() {
        doImport(null, null);
    }

    public final void doImport(@Nullable final String mimeType, @Nullable final String attributes) {
        final File existingFile = null;
        // Diese Zeile muss drin bleiben, weil sonst der Fehler verschwindet
        final ImportData importData = createImportData(mimeType, existingFile, attributes);
    }

    ImportData createImportData(@Nullable final String mimeType, final File existingFile, @Nullable final String attributes) {
        final ClientFileImportObject importObject = new ClientFileImportObject(existingFile, mimeType, attributes);
        return new ImportData(importObject, existingFile);
    }
}
