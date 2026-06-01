package dblearnstar.webapp.services;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.tapestry5.commons.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.apache.commons.text.StringEscapeUtils;

import dblearnstar.model.entities.StudentSubmitSolution;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TestInstanceParameters;
import dblearnstar.model.model.Triplet;
import dblearnstar.webapp.model.dto.TriggerEvaluationResult;
import dblearnstar.webapp.model.dto.TriggerResultForPrinting;
import dblearnstar.webapp.model.dto.TriggerTestCaseResult;

public class TriggerEvaluationServiceImpl implements TriggerEvaluationService {

    @Inject
    private Logger logger;

    @Inject
    private Messages messages;

    @Inject
    private SystemConfigService systemConfigService;

    @Inject
    private Session session;

    @Inject
    private GenericService genericService;

    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
        "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE",
        "COPY", "CREATE ROLE", "CREATE USER", "SET ROLE", "SET SESSION",
        "pg_",
        "UNLOGGED",
        "LISTEN", "NOTIFY"
    );


    public TriggerEvaluationResult evalTriggerForSchema (
        TaskInTestInstance taskInTestInstance,
        TestInstanceParameters testInstanceParameters,
        String trigger,
        Long studentId,
        String evalSchema,
        String taskDescription
    ) {

        List<String> resultsEval = new ArrayList<>();
        List<String> resultsErrors = new ArrayList<>();

        if(!isTriggerSQLSafe(trigger)) {

            resultsErrors.add(messages.get("trigger-db-modifications"));
            logger.error("Student {} trigger unsafe for evaluation: {}", studentId, trigger);
            new TriggerEvaluationResult(resultsEval, resultsErrors, false);
        }

        Connection conn = null;

        try {
            conn = getNewDbConnection(testInstanceParameters);
        } catch (SQLException e) {
            logger.error("Db connection can not be created");
        }

        try {
            boolean doAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            conn.setSchema(evalSchema);

            try (Statement statement = conn.createStatement()) {

                String preparedTrigger = prepareTriggerCurrentTime(taskInTestInstance, trigger, evalSchema);

                try {
                    statement.execute(preparedTrigger);
                } catch (SQLException e) {
                    resultsErrors.add("Trigger execution failed: "+e.getMessage());
                    logger.error("Student {} trigger execution failed:\n Trigger: {}\n Exception: {}",
                        studentId, trigger, e.getMessage());
                }

                List<Triplet<Long, String, String>> testCases = parseTestCases(taskDescription);

                for(Triplet<Long, String, String> testCase: testCases) {

                    if(testCase.getSecondItem().equals("dml")) {

                        int affected = statement.executeUpdate(testCase.getThirdItem());
                        if(affected <= 0) {
                            resultsEval.add(messages.get("trigger-resultIncorrect"));

                            return new TriggerEvaluationResult(resultsEval, resultsErrors, false);
                        }

                    } else if (testCase.getSecondItem().equals("eval")) {

                        try (ResultSet rs = statement.executeQuery(testCase.getThirdItem())) {

                            if(rs.next()) {
                                boolean correctEvaluation = rs.getBoolean(1);
                                if(!correctEvaluation) {
                                    resultsEval.add(messages.get("trigger-resultIncorrect"));

                                    return new TriggerEvaluationResult(resultsEval, resultsErrors, false);
                                }
                            } else {
                                return new TriggerEvaluationResult(resultsEval, resultsErrors, false);
                            }
                        }
                    }
                }

                resultsEval.add(messages.get("trigger-correct"));
                logger.debug("Student {} successful trigger evaluation:\n Trigger: {}", studentId, trigger);
            } finally {

                conn.rollback();
                conn.setAutoCommit(doAutoCommit);
            }
        } catch (SQLException e) {

            resultsErrors.add("Trigger evaluation failed");
            logger.error("Student {} trigger evaluation failed with error:\n Trigger: {} \n Exception: {}",
                studentId, trigger, e.getMessage());
        }
        return new TriggerEvaluationResult(resultsEval, resultsErrors, resultsErrors.isEmpty());
    }

    public TriggerResultForPrinting evalTriggerForPrinting (
        TaskInTestInstance taskInTestInstance,
        TestInstanceParameters testInstanceParameters,
        String trigger,
        Long studentId,
        String evalSchema,
        String taskDescription
    ) {

        List<TriggerTestCaseResult> testCaseResults = new ArrayList<>();
        List<String> resultsErrors = new ArrayList<>();

        if(!isTriggerSQLSafe(trigger)) {

            resultsErrors.add(messages.get("trigger-db-modifications"));
            logger.error("Student {} trigger unsafe for eval and printing: {}", studentId, trigger);
            return new TriggerResultForPrinting(testCaseResults, resultsErrors);
        }

        Connection conn = null;

        try {
            conn = getNewDbConnection(testInstanceParameters);
        } catch (SQLException e) {
            logger.error("Db connection can not be created");
        }

        try {
            boolean doAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            conn.setSchema(evalSchema);

            try (Statement statement = conn.createStatement()) {

                String preparedTrigger = prepareTriggerCurrentTime(taskInTestInstance, trigger, evalSchema);
                try {
                    statement.execute(preparedTrigger);
                } catch (SQLException e) {
                    resultsErrors.add("Trigger execution failed: "+e.getMessage());
                    logger.error("Student {} trigger execution failed:\n Trigger: {}\nException: {}",
                        studentId, trigger, e.getMessage());
                }

                List<Triplet<Long, String, String>> testCases = parseTestCases(taskDescription);

                for(Triplet<Long, String, String> testCase: testCases) {

                    TriggerTestCaseResult testCaseResult = new TriggerTestCaseResult(
                        testCase.getFirstItem(),
                        testCase.getThirdItem()
                    );

                    if(testCase.getSecondItem().equals("action")) {

                        int affected = statement.executeUpdate(testCase.getThirdItem());
                        testCaseResult.setResultCorrect(affected > 0);
                    } else if (testCase.getSecondItem().equals("check")) {

                        try (ResultSet rs = statement.executeQuery(testCase.getThirdItem())) {

                            if(rs.next()) {
                                boolean correctEvaluation = rs.getBoolean(1);
                                testCaseResult.setResultCorrect(correctEvaluation);
                            } else {
                                testCaseResult.setResultCorrect(rs.next());
                            }
                        }
                    }
                    testCaseResults.add(testCaseResult);
                }
            } finally {
                conn.rollback();
                conn.setAutoCommit(doAutoCommit);
            }

        } catch (SQLException e) {

            resultsErrors.add("Trigger evaluation failed");
            logger.error("Student {} trigger evaluation for printing failed with error:\n Trigger: {} \nException: {}",
                studentId, trigger, e.getMessage());
        }
        return new TriggerResultForPrinting(
            testCaseResults,
            resultsErrors
        );
    }

    public void reEvalSolution(String issuedByUserName, StudentSubmitSolution studentSubmission) {

        TaskInTestInstance taskInTestInstance = studentSubmission.getTaskInTestInstance();

        TestInstanceParameters testInstanceParameters = taskInTestInstance.getTestInstance().getTestInstanceParameters().get(0);

        String trigger = studentSubmission.getSubmission();
        long studentId = studentSubmission.getStudentStartedTest().getStudent().getStudentId();
        String taskDescription = taskInTestInstance.getTask().getDescription();

        logger.info("Trigger reevaluation started for studentSubmitSolutionId: {}", studentSubmission.getStudentSubmitSolutionId());

        TriggerEvaluationResult resultSimple = evalTriggerForSchema(
            taskInTestInstance,
            testInstanceParameters,
            trigger,
            studentId,
            testInstanceParameters.getSchemaSimple(),
            taskDescription
        );
        logger.info("Trigger reevaluation simple studentSubmitSolutionId: {} reevaluated as: {}",
            studentSubmission.getStudentSubmitSolutionId(), resultSimple.isTriggerCorrect());

        TriggerEvaluationResult resultComplex = evalTriggerForSchema(
            taskInTestInstance,
            testInstanceParameters,
            trigger,
            studentId,
            testInstanceParameters.getSchemaComplex(),
            taskDescription
        );
        logger.info("Trigger reevaluation complex studentSubmitSolutionId: {} reevaluated as: {}",
            studentSubmission.getStudentSubmitSolutionId(), resultComplex.isTriggerCorrect());

        studentSubmission.setEvaluationSimple(resultSimple.isTriggerCorrect());
        studentSubmission.setEvaluationComplex(resultComplex.isTriggerCorrect());
        studentSubmission.setEvaluationExam(resultSimple.isTriggerCorrect() && resultComplex.isTriggerCorrect());

        getEntityManager().saveOrUpdate(studentSubmission);
    }

    @Override
    public void reEvalListOfSubmissions(String userName, List<StudentSubmitSolution> list) {

        list.forEach(studentSubmitSolution -> {
            try {
                reEvalSolution(userName, studentSubmitSolution);
            } catch (Exception e) {
                logger.error("Fail evaluation for sssId: {}", studentSubmitSolution.getStudentSubmitSolutionId());
            }
        });
    }

    private Connection getNewDbConnection(TestInstanceParameters testInstanceParameters) throws SQLException {

        String url = String.format("jdbc:%s://%s:%s/%s",
            "postgresql" ,
            testInstanceParameters.getHostname(),
            testInstanceParameters.getPort(),
            testInstanceParameters.getDbName()
        );

        return DriverManager.getConnection(
            url,
            testInstanceParameters.getDbUser(),
            testInstanceParameters.getDbPass()
        );
    }

    private Session getEntityManager() {
        return session.getSession();
    }

    private String prepareTriggerCurrentTime(
        TaskInTestInstance taskInTestInstance,
        String trigger,
        String schema
    ) {

        String nowString = systemConfigService.getValue(
            "Task",
            taskInTestInstance.getTask().getTaskId(),
            "TEST_NOW_" + schema
        );

        if (nowString == null) {
            logger.warn("Missing system parameter {}", "TEST_NOW_" + schema);
        }
        logger.debug("TEST_NOW {}", nowString);
        String triggerModified = trigger;
        String replaceString = schema + ".now()";

        if (nowString != null && nowString.length() == 35 && nowString.startsWith("TIMESTAMP '")
                && nowString.endsWith("'")) {

            String checkParse = nowString.substring(11, nowString.length() - 1);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date now = null;

            try {
                now = sdf.parse(checkParse);
                replaceString = nowString;
            } catch (ParseException e) {
                logger.error("Parsing parameter {} failed with error {}", "TEST_NOW_" + schema, e.getMessage());
            }
        }

        triggerModified = Pattern.compile(Pattern.quote("now()"), Pattern.CASE_INSENSITIVE)
            .matcher(triggerModified).replaceAll(replaceString);
        triggerModified = Pattern.compile(Pattern.quote("current_date"), Pattern.CASE_INSENSITIVE)
            .matcher(triggerModified).replaceAll(replaceString);
        logger.debug(
            "Modded trigger for evaluation is \n------------------------------\n{}\n==============================",
            triggerModified);

        return triggerModified;
    }

    private boolean isTriggerSQLSafe(String trigger) {

        if (trigger == null || trigger.isBlank()) {
            return false;
        }

        String normalized = trigger
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);

        if (!normalized.startsWith("CREATE OR REPLACE FUNCTION")) {
            return false;
        }

        return DANGEROUS_KEYWORDS.stream().noneMatch(normalized::contains);
    }

    private List<Triplet<Long, String, String>> parseTestCases(String taskDescription) {

        String testCaseString = taskDescription.replaceAll("<[^>]*>", "").trim();
        testCaseString = StringEscapeUtils.unescapeHtml4(testCaseString);

        testCaseString = testCaseString.split("###")[1];
        testCaseString = testCaseString.split("!!!")[0].strip();

        logger.debug("Parsing test cases for trigger: {}", testCaseString);

        return Arrays.stream(testCaseString.split(";"))
            .map(testCase -> {
                String[] parts = testCase.split(",");
                return new Triplet<>(
                    Long.parseLong(parts[0].strip()),
                    parts[1].strip(),
                    parts[2].strip()
                );
            }).toList();
    }

}
