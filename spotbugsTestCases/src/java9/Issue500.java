import java.util.Map;

class Issue500 {
  String literalOK(Map<String, String> map) {
    return map.get("const");
  }

  String refOK(Map<String, String> map, String key) {
    return map.get(key);
  }

  String stringBuilderOK(Map<String, String> map, String key) {
    return map.get(new StringBuilder(key).append("const").toString());
  }

  String nonConstConcatFails(Map<String, String> map, String key) {
    return map.get(key + "const");
  }
}
