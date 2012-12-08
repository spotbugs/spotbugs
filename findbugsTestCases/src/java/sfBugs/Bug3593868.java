package sfBugs;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;

public class Bug3593868 {

//    public <T> T deSerialize(final File file) throws IntrospectionException, IOException, SAXException {
//        return (T) deSerialize(new FileInputStream(file)); // <== false positive here
//    }
//    public <T> T deSerialize2(final File file) throws IntrospectionException, IOException, SAXException {
//        return (T) deSerialize(new FileInputStream(file)); // <== false positive here
//    }
//    public <T> T deSerialize(final InputStream inputStream) throws IntrospectionException, IOException, SAXException {
//        try {
//            return (T) beanReader.parse(inputStream);
//        } finally {
//            IOUtils.closeQuietly(inputStream);
//        }
//    }
// }
}
