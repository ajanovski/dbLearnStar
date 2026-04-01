package dblearnstar.webapp.model.dto;

import java.util.List;

public class TriggerResultForPrinting {

    List<TriggerTestCaseResult> testCaseResults;
    List<String> executionErrors;

    public TriggerResultForPrinting() {
    }

    public TriggerResultForPrinting(
        List<TriggerTestCaseResult> testCaseResults,
        List<String> executionErrors
    ) {
        this.testCaseResults = testCaseResults;
        this.executionErrors = executionErrors;
    }

    public List<TriggerTestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }

    public List<String> getExecutionErrors() {
        return executionErrors;
    }
}
