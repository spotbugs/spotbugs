package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BugCollectionAnalyser {
    @NonNull
    private final List<Rule> rules = new ArrayList<>();
    @NonNull
    private final List<Result> results = new ArrayList<>();
    @NonNull
    private final Map<String, Integer> typeToIndex = new HashMap<>();
    @NonNull
    private final List<List<Placeholder>> indexToPlaceholders = new ArrayList<>();

    /**
     * Map baseURI to uriBaseId. e.g. {@code "/user/ubuntu/github/spotbugs/" -> "8736793520"}
     */
    @NonNull
    private final Map<String, String> baseToId = new HashMap<>();

    BugCollectionAnalyser(@NonNull BugCollection bugCollection) {
        SourceFinder sourceFinder = bugCollection.getProject().getSourceFinder();
        bugCollection.forEach(bug -> {
            String type = bug.getType();
            int index = typeToIndex.computeIfAbsent(type, (t) -> processRule(bug.getBugPattern()));

            processResult(index, bug, sourceFinder);
        });
    }

    JSONArray getRules() {
        return new JSONArray(rules.stream().map(Rule::toJSONObject).collect(Collectors.toList()));
    }

    JSONArray getResults() {
        return new JSONArray(results.stream().map(Result::toJSONObject).collect(Collectors.toList()));
    }

    @NonNull
    JSONObject getOriginalUriBaseIds() {
        JSONObject result = new JSONObject();
        baseToId.forEach((uri, uriBaseId) -> {
            result.put(uriBaseId, new JSONObject().put("uri", uri));
        });
        return result;
    }

    private void processResult(int index, BugInstance bug, SourceFinder sourceFinder) {
        List<String> arguments = indexToPlaceholders.get(index).stream()
                .map(placeholder -> placeholder.toArgument(bug.getAnnotations(), bug.getPrimaryClass()))
                .collect(Collectors.toList());
        List<Location> locations = new ArrayList<>();
        Location.fromBugInstance(bug, sourceFinder, baseToId).ifPresent(locations::add);
        Result result = new Result(bug.getType(), index, new Message(arguments), locations);
        results.add(result);
    }

    private int processRule(BugPattern bugPattern) {
        assert indexToPlaceholders.size() == rules.size();
        int ruleIndex = rules.size();

        List<Placeholder> placeholders = new ArrayList<>();
        MessageFormat formatter = new MessageFormat(bugPattern.getLongDescription());
        String formattedMessage = formatter.format((Integer index, String key) -> {
            int indexOfPlaceholder = placeholders.size();
            placeholders.add(new Placeholder(index, key));
            return String.format("{%d}", indexOfPlaceholder);
        });
        Rule rule = Rule.fromBugPattern(bugPattern, formattedMessage);
        rules.add(rule);
        indexToPlaceholders.add(placeholders);

        return ruleIndex;
    }
}
