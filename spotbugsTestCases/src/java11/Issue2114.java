import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;

class Issue2114 {
    public static String filesReadString(Path path) throws IOException {
    	return Files.readString(path);
    }
    
    public static List<String> filesReadAllLines(Path path) throws IOException {
    	return Files.readAllLines(path);
    }
    
    public static Path filesWriteString(Path path, CharSequence csq) throws IOException {
    	return Files.writeString(path, csq);
    }
    
    public static BufferedReader filesNewBufferedReader(Path path) throws IOException {
    	return Files.newBufferedReader(path);
    }
    
    public static BufferedWriter filesNewBufferedWriter(Path path) throws IOException {
    	return Files.newBufferedWriter(path);
    }
}
