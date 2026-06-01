package dblearnstar.webapp.model.dto;

public class TriggerTestCaseResult {

    long id;
    String statement;
    boolean isResultCorrect;

    public TriggerTestCaseResult(long id, String statement) {
        this.id = id;
        this.statement = statement;
    }

    public void setResultCorrect(boolean resultCorrect) {
        isResultCorrect = resultCorrect;
    }

    public Long getId() {
        return id;
    }

    public String getStatement() {
        return statement;
    }

    public boolean isResultCorrect() {
        return isResultCorrect;
    }
}
