package ghIssues;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/574">GitHub issue</a>
 */
public final class Issue574Errors {

    @SerializedName("name")
    private String stringA;

    @SerializedName(value="name1", alternate={"name2", "name3"})
    private String stringB;

    public Issue574Errors(String a, String b){
        this.stringA = a;
        this.stringB = b;
    }

    @Test
    public static void testSerializedNameExample1() {
        Issue574Errors target = new Issue574Errors("v1", "v2");
        Gson gson = new Gson();
        String json = gson.toJson(target);
        System.out.println(json);
        String output = "{'name':'v1','name1':'v2'}";
        assertEquals(json, output);
    }
}
