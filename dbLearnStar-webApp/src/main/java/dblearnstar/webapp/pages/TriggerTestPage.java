package dblearnstar.webapp.pages;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.commons.Messages;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.http.services.Context;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.http.services.RequestGlobals;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PersistentLocale;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.ajax.JavaScriptCallback;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;

import dblearnstar.model.entities.*;
import dblearnstar.model.model.ModelConstants;
import dblearnstar.model.model.TaskTypeChecker;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.annotations.StudentPage;
import dblearnstar.webapp.components.ModalBox;
import dblearnstar.webapp.model.dto.TriggerEvaluationResult;
import dblearnstar.webapp.model.dto.TriggerResultForPrinting;
import dblearnstar.webapp.model.dto.TriggerTestCaseResult;
import dblearnstar.webapp.services.ActivityManager;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.PersonManager;
import dblearnstar.webapp.services.TestManager;
import dblearnstar.webapp.services.TranslationService;
import dblearnstar.webapp.services.TriggerEvaluationService;
import dblearnstar.webapp.util.AppConfig;


@AdministratorPage
@StudentPage
@Import(stylesheet = { "QueryTest.css" }, module = { "zoneUpdateEffect", "execSelection", "stillAlive",
        "bootstrap/modal", "bootstrap/collapse" })
public class TriggerTestPage {

    @Property
    @SessionState
    private UserInfo userInfo;

    @Property
    private Boolean accessAllowed;

    @Inject
    private Logger logger;
    @Inject
    private Messages messages;
    @Inject
    private AjaxResponseRenderer ajaxResponseRenderer;
    @Inject
    private TranslationService translationService;
    @Inject
    private PersistentLocale persistentLocale;
    @Inject
    private Request request;
    @Inject
    private RequestGlobals requestGlobals;
    @Inject
    private Context context;
    @Inject
    private JavaScriptSupport javaScriptSupport;

    @Inject
    private GenericService genericService;
    @Inject
    private TestManager testManager;
    @Inject
    private ActivityManager activityManager;
    @Inject
    TriggerEvaluationService triggerEvaluationService;

    @Inject
    private PersonManager pm;

    @InjectComponent
    private Zone historyZone;
    @InjectComponent
    private Zone resultsZone;
    @InjectComponent
    private Zone errorZone;
    @InjectComponent
    private Zone evalZone;
    @InjectComponent
    private Zone currentTimeZone;
    @InjectComponent
    private ModalBox errorModal;

    private Student activeStudent;

    @Property
    @PageActivationContext
    private TaskInTestInstance taskInTestInstance;

    @Property
    @Persist
    private Boolean filterNotForEvalution;

    @Property
    @Persist(PersistenceConstants.FLASH)
    private String message;

    @Property
    @Persist(PersistenceConstants.FLASH)
    private String triggerString;

    @Property
    @Persist
    private String errorPosition;

    @Property
    private String codeType;

    @Property
    private List<String> resultsErrors;
    @Property
    private List<String> resultsEvaluation;

    @Property
    private StudentSubmitSolution historicalSolution;
    @Property
    private Boolean evalResultsSimple;
    @Property
    private Boolean evalResultsComplex;
    @Property
    private Boolean evalResultsExam;

    @Property
    private StudentStartedTest studentStartedTest;

    @Property
    private String evaluationLine;
    @Property
    private String errorLine;
    @Property
    private String oneHeader;

    @Property
    List<TriggerTestCaseResult> testCaseResults;
    @Property
    TriggerTestCaseResult testCaseResult;

    private Long studentId;

    public Object onActivate(EventContext eventContext) {
        if (eventContext.getCount() > 1) {

            return ExamsAndTasksOverviewPage.class;
        } else if (eventContext.getCount() == 1) {
            TaskInTestInstance tti = eventContext.get(TaskInTestInstance.class, 0);
            if (tti == null) {
                logger.error("tti is null, username:{}", userInfo.getUserName());

                return ExamsAndTasksOverviewPage.class;
            } else {
                taskInTestInstance = genericService.getByPK(TaskInTestInstance.class, tti.getTaskInTestInstanceId());
                activeStudent = pm.getStudentsByPersonId(userInfo.getPersonId()).get(0);
                studentId = activeStudent.getStudentId();

                if (userInfo.isAdministrator() || testManager.accessToTaskInTestInstanceAllowed(activeStudent, tti)) {
                    studentId = activeStudent.getStudentId();
                    codeType = taskInTestInstance.getTask().getTaskIsOfTypes().get(0).getTaskType().getCodetype();

                    resultsErrors = null;

                    resultsEvaluation = null;
                    taskInTestInstance = genericService.getByPK(TaskInTestInstance.class,
                        taskInTestInstance.getTaskInTestInstanceId());

                    if (filterNotForEvalution == null) {
                        filterNotForEvalution = false;
                    }
                    recordActivity(activeStudent.getPerson(), ModelConstants.ActivityViewTask, "", "onActivity");
                    logger.debug("access allowed");
                    accessAllowed = true;

                    return null;
                } else {
                    accessAllowed = false;
                    logger.error("Task not allowed: ttiId:{} username:{}", tti.getTaskInTestInstanceId(),
                        userInfo.getUserName());

                    return ExamsAndTasksOverviewPage.class;
                }
            }
        } else {
            logger.error("Task not selected username:{}", userInfo.getUserName());
            return ExamsAndTasksOverviewPage.class; // no tti selected
        }
    }

    public TaskInTestInstance onPassivate() {
        return taskInTestInstance;
    }

    public void setupRender() {

        if (codeType != null) {
            if (TaskTypeChecker.isTRIGGER(codeType)) {
                javaScriptSupport.require("codemirror-run");
                javaScriptSupport.require("codemirror-error");
            }
        }
    }

    public Date getCurrentTime() {
        return new Date();
    }

    @PublishEvent
    @OnEvent("stillAlive")
    public void stillAlive(@RequestParameter(value = "payload") String payload,
                           @RequestParameter(value = "issuer") String issuer) {

        recordActivity(activeStudent.getPerson(), ModelConstants.ActivityStillViewing, payload, issuer);

        if (request.isXHR()) {
            ajaxResponseRenderer.addRender(currentTimeZone);
        }
    }

    @CommitAfter
    public void onSelectedFromEvaluate() {

        recordActivity(activeStudent.getPerson(), ModelConstants.ActivityEval, triggerString, "onSelectedFromEvaluate");
        startTestIfNotStarted();
        runTriggerAndPrintResults(triggerString, false);
        runTriggerAndEval(triggerString);
    }

    @CommitAfter
    public void onSelectedFromRunOnly() {

        startTestIfNotStarted();
        runTriggerAndPrintResults(triggerString, true);
    }

    public void onActionFromLoadHistoricalSolution(StudentSubmitSolution sss) {

        logger.info("Load historical {}", sss.getStudentSubmitSolutionId());

        if (sss.getStudentStartedTest().getStudent().getPerson().getPersonId() == userInfo.getPersonId()) {
            triggerString = sss.getSubmission();
            taskInTestInstance = sss.getTaskInTestInstance();

        } else {
            logger.error(
                "Load historical attempting to load solution from another user: loggedInUser {} - otherUser {}",
                userInfo.getUserName(), sss.getStudentStartedTest().getStudent().getPerson().getUserName()
            );
            userInfo.setUserName(null);
            userInfo.setPersonId(null);
            userInfo.setUserRoles(null);

            throw new RuntimeException("Not allowed. You have been logged out.");
        }
    }

    public void onActionFromFilterNotForEvaluation() {
        filterNotForEvalution = true;
        ajaxResponseRenderer.addRender(historyZone);
    }

    public void onActionFromFilterForEvaluation() {
        filterNotForEvalution = false;
        ajaxResponseRenderer.addRender(historyZone);
    }

    public void onActionFromHideModal() {

        errorModal.hide();
        if (request.isXHR()) {
            ajaxResponseRenderer.addRender(errorZone);
            if(errorPosition != null) {
                ajaxResponseRenderer.addCallback(positionToError());
            }
        }
    }

    public void onActionFromHideEvalModal() {

        errorModal.hide();
        if (request.isXHR()) {
            ajaxResponseRenderer.addRender(evalZone);
            if(errorPosition != null) {
                ajaxResponseRenderer.addCallback(positionToError());
            }
        }
    }

    private void startTestIfNotStarted() {
        studentStartedTest = testManager.studentStartTest(
            studentId,
            taskInTestInstance.getTestInstance().getTestInstanceId()
        );
    }

    @CommitAfter
    public void runTriggerAndEval(String triggerString) {

        logger.debug("Trigger evaluation starting: {}", userInfo.getUserName());

        TestInstanceParameters testInstanceParameters = taskInTestInstance
            .getTestInstance()
            .getTestInstanceParameters()
            .get(0);

        TriggerEvaluationResult resultsSimpleEval = triggerEvaluationService
            .evalTriggerForSchema(
                taskInTestInstance,
                testInstanceParameters,
                triggerString,
                studentId,
                testInstanceParameters.getSchemaSimple(),
                taskInTestInstance.getTask().getDescription()
            );

        TriggerEvaluationResult resultsComplexEval = triggerEvaluationService
            .evalTriggerForSchema(
                taskInTestInstance,
                testInstanceParameters,
                triggerString,
                studentId,
                testInstanceParameters.getSchemaComplex(),
                taskInTestInstance.getTask().getDescription()
            );

        TriggerEvaluationResult resultsExamEval = triggerEvaluationService
            .evalTriggerForSchema(
                taskInTestInstance,
                testInstanceParameters,
                triggerString,
                studentId,
                testInstanceParameters.getSchemaExam(),
                taskInTestInstance.getTask().getDescription()
            );

        evalResultsSimple = resultsSimpleEval.isTriggerCorrect();
        evalResultsComplex = resultsComplexEval.isTriggerCorrect();
        evalResultsExam = resultsExamEval.isTriggerCorrect();

        resultsErrors = resultsSimpleEval.getEvaluationErrors();

        if(resultsErrors.isEmpty()) {
            resultsEvaluation = resultsSimpleEval.getEvaluationResults();
        } else {
            errorPosition = getQueryErrorPosition();
        }

        recordQueryAsStudentSubmitSolution(false, triggerString);

        ajaxResponseRenderer
            .addRender("historyZone", historyZone)
            .addRender("evalZone", evalZone)
            .addRender("errorZone", errorZone);
    }

    @CommitAfter
    public void runTriggerAndPrintResults(String triggerString, boolean notForEvaluation) {

        logger.debug("Trigger execution starting: {}", userInfo.getUserName());

        TestInstanceParameters testInstanceParameters = taskInTestInstance
            .getTestInstance()
            .getTestInstanceParameters()
            .get(0);

        TriggerResultForPrinting evalResults = triggerEvaluationService.evalTriggerForPrinting(
            taskInTestInstance,
            testInstanceParameters,
            triggerString,
            studentId,
            testInstanceParameters.getSchemaSimple(),
            taskInTestInstance.getTask().getDescription()
        );

        testCaseResults = evalResults.getTestCaseResults();
        resultsErrors = evalResults.getExecutionErrors();

        ajaxResponseRenderer
            .addRender("resultsZone", resultsZone);

        if(notForEvaluation) {

            recordQueryAsStudentSubmitSolution(true, triggerString);
            errorPosition = getQueryErrorPosition();
            ajaxResponseRenderer
                .addRender("historyZone", historyZone)
                .addRender("errorZone", errorZone);
        }

    }

    @CommitAfter
    private StudentSubmitSolution recordQueryAsStudentSubmitSolution(Boolean notForEvaluation, String triggerString) {

        if (triggerString != null) {

            StudentSubmitSolution solution = new StudentSubmitSolution();
            solution.setSubmittedOn(getCurrentTime());
            solution.setStudentStartedTest(studentStartedTest);
            solution.setSubmission(triggerString);
            solution.setTaskInTestInstance(taskInTestInstance);
            solution.setEvaluationSimple(evalResultsSimple);
            solution.setEvaluationComplex(evalResultsComplex);
            solution.setEvaluationExam(evalResultsExam);
            solution.setNotForEvaluation(notForEvaluation);

            String clientInfo = "";
            clientInfo = clientInfo + "Headers:\n";
            HttpServletRequest req = requestGlobals.getHTTPServletRequest();
            Enumeration<String> headerNames = req.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                clientInfo = clientInfo + headerName + ": " + req.getHeader(headerName) + "\n";
            }
            solution.setClientInfo(clientInfo);

            String ipAddress = req.getRemoteAddr();
            if (ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("127.0.0.1")) {

                String forwardedFor = req.getHeader("x-forwarded-for");
                if (forwardedFor != null) {
                    ipAddress = forwardedFor;
                }
            }
            solution.setIpAddress(ipAddress);
            genericService.save(solution);

            return solution;
        } else {
            return null;
        }
    }

    @CommitAfter
    public void recordActivity(Person p, String type, String payload, String issuer) {

        logger.debug("recordActivity RECEIVED: {},{},{}", type, issuer, payload);
        activityManager.recordActivityInTask(p, taskInTestInstance, type, payload);
    }

    public boolean isTRIGGER() {
        return TaskTypeChecker.isTRIGGER(codeType);
    }

    public String getTranslateTestInstanceTitle() {

        String translated = translationService.getTranslation("TestInstance", "title",
            taskInTestInstance.getTestInstance().getTestInstanceId(),
            persistentLocale.get().getLanguage().toLowerCase());

        return (translated != null ? translated : taskInTestInstance.getTestInstance().getTitle());
    }

    public String getTranslateTaskTitle() {

        String translated = translationService.getTranslation("Task", "title", taskInTestInstance.getTask().getTaskId(),
            persistentLocale.get().getLanguage().toLowerCase());

        return (translated != null ? translated : taskInTestInstance.getTask().getTitle());
    }

    public String getTranslateTaskDescription() {

        String translated = translationService.getTranslation("Task", "description",
                taskInTestInstance.getTask().getTaskId(), persistentLocale.get().getLanguage().toLowerCase());
        if (translated != null) {
            return parseTaskDescription(translated);
        }
        String description = taskInTestInstance.getTask().getDescription();

        return parseTaskDescription(description);
    }

    private String parseTaskDescription(String description) {
        return description.split("###")[0].trim();
    }

    public String getClassTestIsNow() {

        Date dateFrom = taskInTestInstance.getTestInstance().getScheduledFor();
        Date dateUntil = taskInTestInstance.getTestInstance().getScheduledUntil();

        if (dateFrom != null && dateUntil != null) {
            if (dateFrom.before(getCurrentTime()) && dateUntil.after(getCurrentTime())) {
                if ((new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5))).after(dateUntil)) {
                    return "alert alert-danger";
                } else {
                    return "alert alert-success";
                }
            } else {
                if ((new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5))).before(dateUntil)) {
                    return "alert alert-dark";
                } else {
                    return "";
                }
            }
        } else {
            return "";
        }
    }

    public String getTestIsNow() {

        Date dateFrom = taskInTestInstance.getTestInstance().getScheduledFor();
        Date dateUntil = taskInTestInstance.getTestInstance().getScheduledUntil();

        if (dateFrom != null && dateUntil != null) {
            if (dateFrom.before(getCurrentTime()) && dateUntil.after(getCurrentTime())) {
                if ((new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5))).after(dateUntil)) {
                    return messages.get("timeIsRunningOut-label");
                } else {
                    return messages.get("timeTestIsActive-label");
                }
            } else {
                if ((new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5))).before(dateUntil)) {
                    return messages.get("timehasRunOut-label");
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public String getDisplayEndTime() {

        Date dateFrom = taskInTestInstance.getTestInstance().getScheduledFor();
        Date dateUntil = taskInTestInstance.getTestInstance().getScheduledUntil();

        if (dateFrom != null && dateUntil != null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AppConfig.getString("datetime.gui.format"));
            if (dateFrom.before(getCurrentTime()) && dateUntil.after(getCurrentTime())) {
                if ((new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5))).after(dateUntil)) {
                    return simpleDateFormat.format(dateUntil);
                } else {
                    return simpleDateFormat.format(dateUntil);
                }
            } else {
                if ((new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5))).before(dateUntil)) {
                    return simpleDateFormat.format(dateUntil);
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public String getDisplayCurrentTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AppConfig.getString("datetime.gui.format"));
        return simpleDateFormat.format(getCurrentTime());
    }

    public String getStyleClassForEvaluation() {
        if (evalResultsComplex == null) {
            return " alert-danger errorpanel color-queryError ";
        } else if (!evalResultsComplex) {
            return " alert-danger errorpanel color-queryError ";
        } else {
            return " alert-success  color-queryCorrect ";
        }
    }

    public String getActiveEval() {

        if (filterNotForEvalution) {
            return "";
        } else {
            return "active";
        }
    }

    public String getActiveNotEval() {

        if (filterNotForEvalution) {
            return "active";
        } else {
            return "";
        }
    }

    public String getClassLeft() {
        return "col-lg-6";
    }

    public String getClassRight() {
        return "col-lg-6";
    }

    public String getEditorAreaType() {
        return "SQL";
    }

    public String getShouldBeAsync() {
        return "true";
    }

    private JavaScriptCallback positionToError() {

        return new JavaScriptCallback() {
            public void run(JavaScriptSupport javascriptSupport) {
                javaScriptSupport.require("codemirror-error").invoke("positionToError").with(errorPosition);
            }
        };
    }

    public String getQueryErrorPosition() {

        if (resultsErrors != null && resultsErrors.size() > 0) {
            try {
                List<String> lines = resultsErrors.stream().filter(p -> p.toLowerCase().contains("position"))
                        .collect(Collectors.toList());
                if (lines != null && lines.size() > 0) {
                    String s = lines.get(0);
                    String posit = s.substring(s.indexOf("Position:") + 10).split(" ")[0];
                    return posit;
                } else {
                    return "0";
                }
            } catch (Exception e) {
                return "0";
            }
        } else {
            return "0";
        }
    }

    public List<StudentSubmitSolution> getHistoryOfSolutions() {

        return testManager.getHistoryOfSolutions(
            taskInTestInstance.getTaskInTestInstanceId(),
            filterNotForEvalution,
            studentId
        );
    }

}
