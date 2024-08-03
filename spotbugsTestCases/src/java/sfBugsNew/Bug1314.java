package sfBugsNew;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1314 {
    @ExpectWarning("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public String getFirstChildName(Element e) {
        NodeList list = e.getElementsByTagName("child");
        if(list != null) {
            return ((Element)list.item(0)).getAttribute("name");
        }
        return null;
    }
}
