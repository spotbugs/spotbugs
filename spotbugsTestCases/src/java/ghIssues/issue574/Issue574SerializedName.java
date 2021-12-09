package ghIssues;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/574">GitHub issue</a>
 */
public final class Issue574SerializedName {

    @SerializedName("name")
    private String stringA;

    @SerializedName(value="name1", alternate={"name2", "name3"})
    private String stringB;

    public Issue574SerializedName(String a, String b){
        this.stringA = a;
        this.stringB = b;
    }

    @Test
    public static void testSerializedNameExample1() {
        Issue574SerializedName target = new Issue574SerializedName("v1", "v2");
        Gson gson = new Gson();
        String json = gson.toJson(target);
        System.out.println(json);
        String output = "{'name':'v1','name1':'v2'}";
        assertEquals(json, output);
    }
}
