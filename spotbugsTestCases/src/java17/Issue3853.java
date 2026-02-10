import java.io.FileReader;
import java.io.IOException;

class Issue3853 {
    public static FileReader filesReader(String path) throws IOException {
    	return new FileReader(path);
    }
}
