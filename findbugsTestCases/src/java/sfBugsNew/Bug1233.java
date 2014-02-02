package sfBugsNew;

import com.google.gson.Gson;

public class Bug1233 {

    static class Container {
        public String containdField;
    }

    public String getJSON(Gson gson) {
        Container container = new Container();
        container.containdField = "Some String";  //<--here you get false positive
        return gson.toJson(container);

    }
}
