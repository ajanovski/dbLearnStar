package dblearnstar.webapp.model.dto;

import java.util.List;

public class TriggerEvaluationResult {

    List<String> evaluationResults;
    List<String> evaluationErrors;
    boolean isTriggerCorrect;

    public TriggerEvaluationResult() {
    }

    public TriggerEvaluationResult(
        List<String> evaluationResults,
        List<String> evaluationErrors,
        Boolean isTriggerCorrect
    ) {
        this.evaluationResults = evaluationResults;
        this.evaluationErrors = evaluationErrors;
        this.isTriggerCorrect = isTriggerCorrect;
    }

    public List<String> getEvaluationResults() {
        return evaluationResults;
    }

    public List<String> getEvaluationErrors() {
        return evaluationErrors;
    }

    public boolean isTriggerCorrect() {
        return isTriggerCorrect;
    }
}
