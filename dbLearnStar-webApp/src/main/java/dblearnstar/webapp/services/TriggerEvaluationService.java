package dblearnstar.webapp.services;

import dblearnstar.model.entities.StudentSubmitSolution;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TestInstanceParameters;
import dblearnstar.webapp.model.dto.TriggerEvaluationResult;
import dblearnstar.webapp.model.dto.TriggerResultForPrinting;


public interface TriggerEvaluationService {

    /**
     * Evaluates a trigger for a given task and schema, producing detailed results for each test case.
     * Used when printing or displaying the evaluation of each test case.
     *
     * @param taskInTestInstance the task instance being evaluated
     * @param testInstanceParameters parameters for test instance
     * @param trigger the SQL trigger submitted by the student
     * @param studentId the ID of the student submitting the trigger
     * @param evalSchema the database schema to run the evaluation against
     * @param taskDescription the task description containing test cases to be parsed and executed
     * @return a TriggerResultForPrinting object containing the list of test case results
     *         and any errors encountered during evaluation
     */
    TriggerResultForPrinting evalTriggerForPrinting(
        TaskInTestInstance taskInTestInstance,
        TestInstanceParameters testInstanceParameters,
        String trigger,
        Long studentId,
        String evalSchema,
        String taskDescription
    );

    /**
     * Evaluates a trigger for a given task and schema, producing an overall evaluation result.
     * Used for determining if the trigger passes the required test cases for evaluation.
     *
     * @param taskInTestInstance the task instance being evaluated
     * @param testInstanceParameters parameters for test instance
     * @param trigger the SQL trigger submitted by the student
     * @param studentId the ID of the student submitting the trigger
     * @param evalSchema the database schema to run the evaluation against
     * @param taskDescription the task description containing test cases to be parsed and executed
     * @return a TriggerEvaluationResult object containing evaluation results,
     *         any errors encountered, and a boolean indicating overall success
     */
    TriggerEvaluationResult evalTriggerForSchema(
        TaskInTestInstance taskInTestInstance,
        TestInstanceParameters testInstanceParameters,
        String trigger,
        Long studentId,
        String evalSchema,
        String taskDescription
    );

    /**
     * Reevaluates trigger for simple and complex schema and updates results
     *
     * @param issuedByUserName user issuing reevaluation
     * @param studentSubmission student submission object to be updated
     */
    void reEvalSolution(String issuedByUserName, StudentSubmitSolution studentSubmission);

}
