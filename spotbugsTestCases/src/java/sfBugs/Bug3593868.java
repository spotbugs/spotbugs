package sfBugs;


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
