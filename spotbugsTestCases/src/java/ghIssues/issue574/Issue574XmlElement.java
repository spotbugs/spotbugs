package ghIssues;

import org.junit.Test;

import javax.xml.bind.annotation.*;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/574">GitHub issue</a>
 */

@XmlRootElement
public final class Issue574XmlElement {

    @XmlAttribute
    private String type;

    @XmlAttribute
    private String gender;

    @XmlValue
    private String description;

    public Issue574XmlElement(String type, String gender, String description){
        this.type = type;
        this.gender = gender;
        this.description = description;
    }

    @Test
    public void test1(){
        Issue574XmlElement testObject = new Issue574XmlElement("test","male","swimming");
    }

}
