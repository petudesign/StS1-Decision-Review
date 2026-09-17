package spirecoach.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A player-facing explanation produced after the decision has already happened. */
public final class Lesson {
    private final String ruleId;
    private final String title;
    private final String explanation;
    private final String principle;
    private final Confidence confidence;
    private final int impact;
    private final List<String> relevantActions;
    private final List<String> alternativeActions;
    private final Map<String, Integer> values;

    public Lesson(String ruleId, String title, String explanation, String principle,
                  Confidence confidence, int impact, List<String> relevantActions,
                  Map<String, Integer> values) {
        this(ruleId, title, explanation, principle, confidence, impact,
                relevantActions, Collections.<String>emptyList(), values);
    }

    public Lesson(String ruleId, String title, String explanation, String principle,
                  Confidence confidence, int impact, List<String> relevantActions,
                  List<String> alternativeActions, Map<String, Integer> values) {
        this.ruleId = ruleId;
        this.title = title;
        this.explanation = explanation;
        this.principle = principle;
        this.confidence = confidence;
        this.impact = impact;
        this.relevantActions = relevantActions == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(relevantActions));
        this.alternativeActions = alternativeActions == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(alternativeActions));
        this.values = values == null
                ? Collections.<String, Integer>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(values));
    }

    public String getRuleId() { return ruleId; }
    public String getTitle() { return title; }
    public String getExplanation() { return explanation; }
    public String getPrinciple() { return principle; }
    public Confidence getConfidence() { return confidence; }
    public int getImpact() { return impact; }
    public List<String> getRelevantActions() { return relevantActions; }
    public List<String> getAlternativeActions() { return alternativeActions; }
    public Map<String, Integer> getValues() { return values; }
}
