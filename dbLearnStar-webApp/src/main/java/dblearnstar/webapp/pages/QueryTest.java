/*******************************************************************************
 * Copyright (C) 2021 Vangel V. Ajanovski
 *     
 * This file is part of the dbLearnStar system (hereinafter: dbLearn*).
 *     
 * dbLearn* is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 *     
 * dbLearn* is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
 * details.
 *     
 * You should have received a copy of the GNU General Public License along 
 * with dbLearn*.  If not, see <https://www.gnu.org/licenses/>.
 * 
 ******************************************************************************/

package dblearnstar.webapp.pages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.PublishEvent;
import org.apache.tapestry5.annotations.RequestParameter;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.beaneditor.Validate;
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
import org.apache.tapestry5.upload.services.UploadedFile;
import org.slf4j.Logger;

import dblearnstar.model.entities.Person;
import dblearnstar.model.entities.Student;
import dblearnstar.model.entities.StudentStartedTest;
import dblearnstar.model.entities.StudentSubmitSolution;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TestInstanceParameters;
import dblearnstar.model.model.ModelConstants;
import dblearnstar.model.model.TaskTypeChecker;
import dblearnstar.model.model.Triplet;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.annotations.StudentPage;
import dblearnstar.webapp.components.ModalBox;
import dblearnstar.webapp.services.ActivityManager;
import dblearnstar.webapp.services.EvaluationService;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.PersonManager;
import dblearnstar.webapp.services.SystemConfigService;
import dblearnstar.webapp.services.TestManager;
import dblearnstar.webapp.services.TranslationService;
import dblearnstar.webapp.util.AppConfig;
import dblearnstar.webapp.util.FileStreamResponse;
import jakarta.servlet.http.HttpServletRequest;

@AdministratorPage
@StudentPage
@Import(stylesheet = { "QueryTest.css" }, module = { "zoneUpdateEffect", "execSelection", "stillAlive",
		"bootstrap/modal", "bootstrap/collapse" })
public class QueryTest {
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

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@Inject
	private GenericService genericService;
	@Inject
	private TestManager testManager;
	@Inject
	private ActivityManager activityManager;
	@Inject
	private EvaluationService evaluationService;
	@Inject
	private PersonManager pm;
	@Inject
	private SystemConfigService systemConfigService;

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
	private Zone formZone;

	@InjectComponent
	private ModalBox errorModal;

	private Student activeStudent;

	@Property
//	@Persist
	private TaskInTestInstance taskInTestInstance;

	@Property
	@Persist
	private Boolean filterNotForEvalution;

	@Property
	@Persist
	private UploadedFile file;

	@Property
	@Persist
	private String fileComment;

	@Property
	@Persist(PersistenceConstants.FLASH)
	private String message;

	@Property
	@Persist(PersistenceConstants.FLASH)
	private String queryString;

	@Property
	@Persist
	private String errorPosition;

	@Property
	private String codeType;

	@Property
	private List<Object[]> resultsSimple;
	@Property
	private List<String> resultsHeadersSimple;
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
	private Object[] oneRow;
	@Property
	private Object oneColumn;
	@Property
	private StudentStartedTest studentStartedTest;

	@Property
	private String evaluationLine;
	@Property
	private String errorLine;
	@Property
	private String oneHeader;

	private Long studentId;
	private boolean toUpload;
	private boolean toSubmitText;

	@Persist
	@Property
	@Validate("required")
	private String editorTheme;

	@PublishEvent
	@OnEvent("stillAlive")
	public void stillAlive(@RequestParameter(value = "payload") String payload,
			@RequestParameter(value = "issuer") String issuer) {
		toUpload = false;
		toSubmitText = false;
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivityStillViewing, payload, issuer);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(currentTimeZone);
		}
	}

	@PublishEvent
	@OnEvent("execSelection")
	public void execRunSelection(@RequestParameter(value = "query") String query,
			@RequestParameter(value = "issuer") String issuer) {
		toUpload = false;
		toSubmitText = false;
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivityExecSelection, query, issuer);
		runQueryAndEval(true, query); // field is NotForEvaluation
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(historyZone).addRender(evalZone).addRender(errorZone).addRender(resultsZone);
		}
	}

	@PublishEvent
	@OnEvent("execAll")
	public void execRunAll(@RequestParameter(value = "query") String query,
			@RequestParameter(value = "issuer") String issuer) {
		toUpload = false;
		toSubmitText = false;
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivityExecAll, query, issuer);
		runQueryAndEval(true, query); // field is NotForEvaluation
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(historyZone).addRender(evalZone).addRender(errorZone).addRender(resultsZone);
		}
	}

	@PublishEvent
	@OnEvent("evalAll")
	public void execEvalAll(@RequestParameter(value = "query") String query,
			@RequestParameter(value = "issuer") String issuer) {
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivityEval, query, issuer);
	}

	@PublishEvent
	@OnEvent("plan")
	public void execPlan(@RequestParameter(value = "query") String query,
			@RequestParameter(value = "issuer") String issuer) {
		toUpload = false;
		toSubmitText = false;
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivityPlan, query, issuer);
		runQueryAndEval(true, "explain " + query); // field is
													// NotForEvaluation
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(historyZone).addRender(evalZone).addRender(errorZone).addRender(resultsZone);
		}
	}

	@PublishEvent
	@OnEvent("changeTheme")
	@CommitAfter
	public void changeTheme(@RequestParameter(value = "newTheme") String newTheme) {
		toUpload = false;
		toSubmitText = false;
		editorTheme = newTheme;
		if (newTheme != null) {
			systemConfigService.setValueAndCode(ModelConstants.SystemParameterUserPreferences, userInfo.getPersonId(),
					ModelConstants.SystemParameterTypeCodeMirrorTheme, newTheme, newTheme);
		}
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(historyZone).addRender(evalZone).addRender(errorZone).addRender(resultsZone);
		}
	}

	public void onActionFromHideModal() {
		errorModal.hide();
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(errorZone).addCallback(positionToError());
		}
	}

	private JavaScriptCallback positionToError() {
		return new JavaScriptCallback() {
			public void run(JavaScriptSupport javascriptSupport) {
				javaScriptSupport.require("codemirror-error").invoke("positionToError").with(errorPosition);
			}
		};
	}

	public void onActionFromHideEvalModal() {
		errorModal.hide();
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(evalZone).addCallback(positionToError());
		}
	}

	public Object onActivate(EventContext eventContext) {
		if (eventContext.getCount() > 1) {
			return ExamsAndTasksOverviewPage.class;
		} else if (eventContext.getCount() == 1) {
			if (editorTheme == null) {
				String theme = systemConfigService.getValue(ModelConstants.SystemParameterUserPreferences,
						userInfo.getPersonId(), ModelConstants.SystemParameterTypeCodeMirrorTheme);
				if (theme == null) {
					editorTheme = "default";
				} else {
					editorTheme = theme;
				}
			}
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
					resultsSimple = null;

					resultsEvaluation = null;
					resultsHeadersSimple = null;
					taskInTestInstance = genericService.getByPK(TaskInTestInstance.class,
							taskInTestInstance.getTaskInTestInstanceId());
					if (filterNotForEvalution == null) {
						filterNotForEvalution = false;
					}
					recordActivity(activeStudent.getPerson(), ModelConstants.ActivityViewTask, "", "onActivity");
					toUpload = false;
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

	void setupRender() {
		if (codeType != null) {
			if (isSQL()) {
				javaScriptSupport.require("codemirror-run").invoke("initialize").with(editorTheme);
				javaScriptSupport.require("codemirror-error");
			}
		}
	}

	@CommitAfter
	public void recordActivity(Person p, String type, String payload, String issuer) {
		logger.debug("recordActivity RECEIVED: {},{},{}", type, issuer, payload);
		activityManager.recordActivityInTask(p, taskInTestInstance, type, payload);
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

	private void startTestIfNotStarted() {
		studentStartedTest = testManager.studentStartTest(studentId,
				taskInTestInstance.getTestInstance().getTestInstanceId());
	}

	private StudentSubmitSolution recordQueryAsStudentSubmitSolution(Boolean notForEvaluation, String query) {
		if (query != null) {
			StudentSubmitSolution solution = new StudentSubmitSolution();
			solution.setSubmittedOn(getCurrentTime());
			solution.setStudentStartedTest(studentStartedTest);
			solution.setSubmission(query);
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
	public void runQueryAndEval(Boolean notForEvaluation, String queryToRun) {
		logger.info("Evaluation starting - notForEval: {} {}", userInfo.getUserName(), notForEvaluation);
		TestInstanceParameters testInstanceParameters = taskInTestInstance.getTestInstance().getTestInstanceParameters()
				.get(0);

		resultsSimple = new ArrayList<Object[]>();
		resultsHeadersSimple = new ArrayList<String>();

		resultsErrors = new ArrayList<String>();
		resultsEvaluation = new ArrayList<String>();

		evalResultsSimple = false;
		evalResultsComplex = false;
		evalResultsExam = false;

		// For printing purposes
		Triplet<List<Object[]>, List<String>, List<String>> rezultatiteZaListanje = evaluationService
				.getResultsForPrintingPurposes(userInfo.getUserName(), queryToRun, testInstanceParameters,
						testInstanceParameters.getSchemaSimple(), "simple");
		resultsSimple = rezultatiteZaListanje.getFirstItem();
		resultsHeadersSimple = rezultatiteZaListanje.getSecondItem();
		resultsErrors = rezultatiteZaListanje.getThirdItem();

		errorPosition = getQueryErrorPosition();

		// For evaluation
		if (!notForEvaluation) {
			if (resultsErrors.isEmpty()) {
				Triplet<List<String>, List<String>, Boolean> evaluacijaSimple = evaluationService.evalResultsIn(
						userInfo.getUserName(), queryToRun, taskInTestInstance, testInstanceParameters,
						testInstanceParameters.getSchemaSimple());
				Triplet<List<String>, List<String>, Boolean> evaluacijaComplex = evaluationService.evalResultsIn(
						userInfo.getUserName(), queryToRun, taskInTestInstance, testInstanceParameters,
						testInstanceParameters.getSchemaComplex());
				resultsEvaluation = evaluacijaSimple.getFirstItem();
				resultsErrors = evaluacijaSimple.getSecondItem();

				evalResultsSimple = evaluacijaSimple.getThirdItem();
				evalResultsComplex = evaluacijaComplex.getThirdItem();
				evalResultsExam = evalResultsSimple && evalResultsComplex;
			}
		}
		startTestIfNotStarted();
		recordQueryAsStudentSubmitSolution(notForEvaluation, queryToRun);
	}

	public void onSelectedFromEvaluate() {
		toUpload = false;
		toSubmitText = false;
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivityEval, queryString, "onSelectedFromEvaluate");
		runQueryAndEval(false, queryString); // field is NotForEvaluation
	}

	public void onSelectedFromUpload() {
		logger.debug("upload clicked");
		toUpload = true;
		toSubmitText = false;
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivityTryUpload, "", "onSelectedFromUpload");
		// doTheUpload();
	}

	public void onSelectedFromSubmitTextSolution() {
		logger.debug("submit clicked");
		toUpload = false;
		toSubmitText = true;
		recordActivity(activeStudent.getPerson(), ModelConstants.ActivitySubmitText, "",
				"onSelectedFromSubmitTextSolution");
	}

	public void onValidateFromQueryTestForm() {
		if (isUPLOAD()) {
			if (file != null) {
				logger.info("File: {}", file.getFileName());
			} else {
				logger.error("upload failed");
			}
		} else {
			logger.debug("onValidate");
		}
	}

	public List<StudentSubmitSolution> getHistoryOfSolutions() {
		return testManager.getHistoryOfSolutions(taskInTestInstance.getTaskInTestInstanceId(), filterNotForEvalution,
				studentId);
	}

	public void onActionFromLoadHistoricalSolution(StudentSubmitSolution sss) {
		logger.info("Load historical {}", sss.getStudentSubmitSolutionId());
		if (sss.getStudentStartedTest().getStudent().getPerson().getPersonId() == userInfo.getPersonId()) {
			queryString = sss.getSubmission();
			taskInTestInstance = sss.getTaskInTestInstance();
		} else {
			logger.error(
					"Load historical attempting to load solution from another user: loggedInUser {} - otherUser {}",
					userInfo.getUserName(), sss.getStudentStartedTest().getStudent().getPerson().getUserName());
			userInfo.setUserName(null);
			userInfo.setPersonId(null);
			userInfo.setUserRoles(null);
			throw new RuntimeException("Not allowed. You have been logged out.");
		}
	}

	public StreamResponse onActionFromDownloadFile(StudentSubmitSolution sss) {
		logger.info("Start Download of {}", sss.getStudentSubmitSolutionId());
		if (sss.getStudentStartedTest().getStudent().getPerson().getPersonId() == userInfo.getPersonId()) {
			queryString = "";
			fileComment = "";
			taskInTestInstance = sss.getTaskInTestInstance();

			String subm = sss.getSubmission();
			String[] listSubm = subm.split("#");
			String origFileName = "";
			String savedFileName = "";
			for (String s : listSubm) {
				if (s.startsWith("FILE:")) {
					origFileName = s;
				}
				if (s.startsWith("SAVEDFILE:")) {
					savedFileName = s;
				}
			}
			savedFileName = savedFileName.replace("SAVEDFILE:", "");
			origFileName = origFileName.replace("FILE:", "");

			File file = new File(AppConfig.getString("additionalFiles.path")
					+ AppConfig.getString("upload.path.submissions") + "/" + savedFileName);

			try {
				FileInputStream fis = new FileInputStream(file);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				try {
					for (int readNum; (readNum = fis.read(buf)) != -1;) {
						bos.write(buf, 0, readNum);
						logger.info("read {} bytes,", readNum);
					}
				} catch (IOException ex) {
					logger.error(ex.getMessage());
				}

				ByteArrayInputStream bais = new ByteArrayInputStream(bos.toByteArray());
				InputStream is = bais;
				return new FileStreamResponse(is, origFileName);
			} catch (Exception e) {
				logger.error(e.getMessage());
				return null;
			}
		} else {
			logger.error("Attempting to download submitted solution from another user: loggedInUser {} - otherUser {}",
					userInfo.getUserName(), sss.getStudentStartedTest().getStudent().getPerson().getUserName());
			return null;
		}
	}

	Object onActionFromBackToExamsOverview() {
		return ExamsAndTasksOverviewPage.class;
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

	public String getActiveNotEval() {
		if (filterNotForEvalution) {
			return "active";
		} else {
			return "";
		}
	}

	public String getActiveEval() {
		if (filterNotForEvalution) {
			return "";
		} else {
			return "active";
		}
	}

	void onActionFromFilterNotForEvaluation() {
		filterNotForEvalution = true;
		ajaxResponseRenderer.addRender(historyZone);
	}

	void onActionFromFilterForEvaluation() {
		filterNotForEvalution = false;
		ajaxResponseRenderer.addRender(historyZone);
	}

	public String getClassLeft() {
		if (TaskTypeChecker.isSQL(codeType)) {
			return "col-lg-6";
		} else if (TaskTypeChecker.isUPLOAD(codeType)) {
			return "col-lg-9";
		} else if (TaskTypeChecker.isTEXT(codeType)) {
			return "col-lg-6";
		} else if (TaskTypeChecker.isDDL(codeType)) {
			return "col-lg-11";
		} else {
			return "col-lg-6";
		}
	}

	public String getClassRight() {
		if (TaskTypeChecker.isSQL(codeType)) {
			return "col-lg-6";
		} else if (TaskTypeChecker.isUPLOAD(codeType)) {
			return "col-lg-3";
		} else if (TaskTypeChecker.isTEXT(codeType)) {
			return "col-lg-6";
		} else if (TaskTypeChecker.isDDL(codeType)) {
			return "col-lg-1";
		} else {
			return "col-lg-6";
		}
	}

	Object onUploadException(FileUploadException ex) {
		message = "I was not able to upload the file";
		logger.error("Upload problem: {}", message);
		return this;
	}

	@CommitAfter
	public void onSuccess() {
		logger.debug("onSuccess");
		if (toUpload) {
			logger.info("Processing upload");
			startTestIfNotStarted();
			evalResultsSimple = false;
			evalResultsComplex = false;
			evalResultsExam = false;
			queryString = "FILE:" + file.getFileName();
			String emde5 = "";
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				md.update(file.getStream().readAllBytes());
				byte[] hashBytes = md.digest();
				StringBuilder sb = new StringBuilder();
				for (byte b : hashBytes) {
					sb.append(String.format("%02x", b));
				}
				emde5 = sb.toString();
			} catch (Exception e) {
				emde5 = null;
			}
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
					AppConfig.getString("date.upload.submission.format"));
			StudentSubmitSolution solution = recordQueryAsStudentSubmitSolution(false, queryString);
			String userName = userInfo.getUserName();
			String fileName = AppConfig.getString("additionalFiles.path")
					+ AppConfig.getString("upload.path.submissions") + "/STUDENT_" + userName + "_SUBMISSION_"
					+ solution.getStudentSubmitSolutionId() + "_" + simpleDateFormat.format(new Date()) + "_MD5_"
					+ emde5;
			try {
				File copied = new File(fileName);
				file.write(copied);
				solution.setSubmission(
						solution.getSubmission() + "#COMMENT:" + fileComment + " #SAVEDFILE:" + copied.getName());
				genericService.saveOrUpdate(solution);
			} catch (Exception ex) {
				logger.error("{}", ex);
			}
			toUpload = false;
		}
		if (toSubmitText) {
			startTestIfNotStarted();
			evalResultsSimple = false;
			evalResultsComplex = false;
			evalResultsExam = false;
			recordQueryAsStudentSubmitSolution(false, queryString);
		}
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(historyZone).addRender(evalZone).addRender(errorZone).addRender(resultsZone);
		}
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
		return (translated != null ? translated : taskInTestInstance.getTask().getDescription());
	}

	public String getShouldBeAsync() {
		if (codeType.equals(ModelConstants.TaskCodeUPLOAD)) {
			return "false";
		} else {
			return "true";
		}
	}

	public String getFileFromSubmission() {
		String subm = historicalSolution.getSubmission();
		String[] listSubm = subm.split("#");
		String output = "";
		for (String s : listSubm) {
			if (s.startsWith("FILE:")) {
				output += "<p>" + s + "</p>";
			}
			if (s.startsWith("COMMENT:")) {
				output += "<p>" + s + "</p>";
			}
		}
		return output;
	}

	public boolean isSQL() {
		return TaskTypeChecker.isSQL(codeType);
	}

	public boolean isTEXT() {
		return TaskTypeChecker.isTEXT(codeType) || TaskTypeChecker.isDDL(codeType);
	}

	public boolean isDDL() {
		return TaskTypeChecker.isDDL(codeType);
	}

	public boolean isUPLOAD() {
		return TaskTypeChecker.isUPLOAD(codeType);
	}

	public String getEditorAreaType() {
		if (isSQL()) {
			return "SQL";
		} else if (isTEXT()) {
			return "CK";
		} else {
			return "";
		}
	}

	public Date getCurrentTime() {
		return new Date();
	}

	public String getDisplayCurrentTime() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AppConfig.getString("datetime.gui.format"));
		return simpleDateFormat.format(getCurrentTime());
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

}
