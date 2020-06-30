package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONArray;

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

    BugCollectionAnalyser(@NonNull BugCollection bugCollection) {
        bugCollection.forEach(bug -> {
            String type = bug.getType();
            int index = typeToIndex.computeIfAbsent(type, (t) -> processRule(t, bug.getBugPattern()));

            processResult(index, bug);
        });
    }

    JSONArray getRules() {
        JSONArray array = new JSONArray();
        for (Rule rule : rules) {
            array.put(rule.toJSONObject());
        }
        return array;
    }

    JSONArray getResults() {
        JSONArray array = new JSONArray();
        for (Result result : results) {
            array.put(result.toJSONObject());
        }
        return array;
    }

    private void processResult(int index, BugInstance bug) {
        List<String> arguments = bug.getAnnotations().stream().map(annotation -> annotation.format("", null)).collect(Collectors.toList());
        Result result = new Result(bug.getType(), index, new Message(arguments));
        results.add(result);
    }

    private int processRule(String type, BugPattern bugPattern) {
        int index = rules.size();
        Rule rule = new Rule(type, bugPattern.getShortDescription(), bugPattern.getLongDescription());
        rules.add(rule);
        return index;
    }
}
