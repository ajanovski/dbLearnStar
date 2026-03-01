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

package dblearnstar.webapp.pages.admin;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.beaneditor.RelativePosition;
import org.apache.tapestry5.beanmodel.BeanModel;
import org.apache.tapestry5.beanmodel.services.BeanModelSource;
import org.apache.tapestry5.beanmodel.services.PropertyConduitSource;
import org.apache.tapestry5.commons.Messages;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PersistentLocale;
import org.apache.tapestry5.services.SelectModelFactory;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.slf4j.Logger;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.github.vertical_blank.sqlformatter.languages.Dialect;

import dblearnstar.model.entities.Person;
import dblearnstar.model.entities.SolutionAssessment;
import dblearnstar.model.entities.Student;
import dblearnstar.model.entities.StudentSubmitSolution;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TestInstance;
import dblearnstar.model.entities.TestInstanceParameters;
import dblearnstar.model.model.ModelConstants;
import dblearnstar.model.model.TaskTypeChecker;
import dblearnstar.model.model.Triplet;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.model.dto.TriggerResultForPrinting;
import dblearnstar.webapp.model.dto.TriggerTestCaseResult;
import dblearnstar.webapp.model.StudentSelectModel;
import dblearnstar.webapp.model.TaskInTestInstanceSelectModel;
import dblearnstar.webapp.services.EvaluationService;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.PersonManager;
import dblearnstar.webapp.services.TestManager;
import dblearnstar.webapp.services.TranslationService;
import dblearnstar.webapp.services.UsefulMethods;
import dblearnstar.webapp.services.TriggerEvaluationService;

@AdministratorPage
@Import(stylesheet = { "SubmissionEvaluations.css", "feedback-styles.css" }, module = { "zoneUpdateEffect",
		"bootstrap/modal", "bootstrap/collapse" })
public class SubmissionEvaluations {

	@SessionState
	private UserInfo userInfo;

	@Inject
	private Logger logger;
	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;

	@Inject
	private BeanModelSource beanModelSource;
	@Inject
	private Messages messages;
	@Inject
	private PropertyConduitSource pcs;

	@Inject
	private SelectModelFactory selectModelFactory;
	@Inject
	private Request request;
	@Inject
	private PersistentLocale persistentLocale;

	@Inject
	private EvaluationService evaluationService;
	@Inject
	private TriggerEvaluationService triggerEvaluationService;
	@Inject
	private GenericService genericService;
	@Inject
	private TestManager testManager;
	@Inject
	private PersonManager personManager;
	@Inject
	private TranslationService translationService;

	@InjectComponent
	private Zone zTask;
	@InjectComponent
	private Zone zStudent;
	@InjectComponent
	private Zone zSubmissions;
	@InjectComponent
	private Zone zModal;
	@InjectComponent
	private Zone zSQLEval;
	@InjectComponent
	private Zone zSolution;

	@Persist
	@Property
	private Student filterStudent;
	@Persist
	@Property
	private TestInstance filterTestInstance;
	@Persist
	@Property
	private TaskInTestInstance filterTaskInTestInstance;
	@Persist
	@Property
	private Boolean onlyEval;
	@Persist
	@Property
	private Boolean onlyLast;
	@Persist
	@Property
	private Boolean onlyCorrect;
	@Persist
	@Property
	private Boolean onlyDateOfExam;
	@Persist
	@Property
	private Boolean onlyAssessed;
	@Persist
	@Property
	private Boolean onlyUnAssessed;
	@Persist
	@Property
	private Boolean hideClientInfo;
	@Persist
	@Property
	private Boolean hideNames;
	@Persist
	@Property
	SolutionAssessment editedAssessment;
	@Persist
	@Property
	private Boolean prettyFormat;
	@Property
	@Persist
	private List<String> resultsHeaders1;
	@Property
	@Persist
	private List<String> resultsHeaders2;
	@Property
	@Persist
	List<TriggerTestCaseResult> triggerResultsSimple;
	@Property
	@Persist
	List<TriggerTestCaseResult> triggerResultsComplex;
	@Property
	@Persist
	private List<Object[]> resultsEvaluation1;
	@Property
	@Persist
	private List<Object[]> resultsEvaluation2;
	@Property
	@Persist
	private List<String> resultsErrors1;
	@Property
	@Persist
	private List<String> resultsErrors2;

	@Property
	private StudentSubmitSolution submission;
	@Property
	private String resultsError;
	@Property
	private String oneHeader;
	@Property
	private Object[] oneRow;
	@Property
	private Object oneColumn;
	@Property
	private TriggerTestCaseResult triggerTestCaseResult;

	public void onActivate() {
		logger.warn("Activated from {} by {} {}", request.getRemoteHost(), userInfo.getUserName(),
				request.getHeader("User-Agent"));
		if (onlyLast == null) {
			onlyLast = true;
		}
		if (onlyEval == null) {
			onlyEval = true;
		}
		if (hideClientInfo == null) {
			hideClientInfo = true;
		}
		if (hideNames == null) {
			hideNames = true;
		}
		if (filterStudent != null) {
			filterStudent = genericService.getByPK(Student.class, filterStudent.getStudentId());
		}
		if (filterTestInstance != null) {
			filterTestInstance = genericService.getByPK(TestInstance.class, filterTestInstance.getTestInstanceId());
		}
		if (filterTaskInTestInstance != null) {
			filterTaskInTestInstance = genericService.getByPK(TaskInTestInstance.class,
					filterTaskInTestInstance.getTaskInTestInstanceId());
		}
		if (prettyFormat == null) {
			prettyFormat = false;
		}
	}

	public void onActivate(SolutionAssessment sa) {
		logger.warn("Activated with sa={} from {} by {} {}", sa, request.getRemoteHost(), userInfo.getUserName(),
				request.getHeader("User-Agent"));
		editedAssessment = genericService.getByPK(SolutionAssessment.class, sa.getSolutionAssessmentId());
	}

	public List<StudentSubmitSolution> getAllSubmissions() {
		List<StudentSubmitSolution> lista = null;
		if (filterTestInstance != null) {
			if (onlyLast != null && onlyLast) {
				lista = evaluationService.getOnlyLastSubmissionsByStudentAndTaskInTestInstance(filterStudent,
						filterTestInstance, filterTaskInTestInstance, onlyEval, onlyCorrect);
			} else {
				lista = evaluationService.getSubmissionsByStudentAndTaskInTestInstance(filterStudent,
						filterTestInstance, filterTaskInTestInstance, onlyEval, onlyCorrect);
			}
		}
		if (lista != null && onlyDateOfExam != null && onlyDateOfExam) {
			lista = lista.stream().filter(sss -> sss.getSubmittedOn()
					.after(sss.getTaskInTestInstance().getTestInstance().getScheduledFor())
					&& sss.getSubmittedOn().before(sss.getTaskInTestInstance().getTestInstance().getScheduledUntil()))
					.collect(Collectors.toList());
		}
		if (lista != null && onlyAssessed != null && onlyAssessed) {
			lista = lista.stream().filter(sss -> (sss.getEvaluations() != null && sss.getEvaluations().size() > 0))
					.collect(Collectors.toList());
		}
		if (lista != null && onlyUnAssessed != null && onlyUnAssessed) {
			lista = lista.stream().filter(sss -> (sss.getEvaluations() == null || sss.getEvaluations().size() == 0))
					.collect(Collectors.toList());
		}

		return lista;
	}

	public BeanModel<StudentSubmitSolution> getModelSSS() {
		BeanModel<StudentSubmitSolution> modelSSS = beanModelSource.createDisplayModel(StudentSubmitSolution.class,
				messages);
		modelSSS.add(RelativePosition.BEFORE, "submission", "submittedBy",
				pcs.create(StudentSubmitSolution.class, "studentStartedTest.student.person.lastName"));
		modelSSS.add(RelativePosition.BEFORE, "submission", "task",
				pcs.create(StudentSubmitSolution.class, "taskInTestInstance.task.title"));
		modelSSS.add(RelativePosition.BEFORE, "submission", "test",
				pcs.create(StudentSubmitSolution.class, "taskInTestInstance.testInstance.title"));
		modelSSS.addEmpty("assessment");
		modelSSS.reorder("submittedBy", "task", "submission", "submittedOn", "evaluationSimple", "evaluationComplex",
				"evaluationExam", "notForEvaluation", "assessment", "ipAddress");
		if (filterStudent != null) {
			modelSSS.exclude("submittedBy");
		}
		if (filterTestInstance != null) {
			modelSSS.exclude("test");
		}
		if (onlyCorrect != null && onlyCorrect) {
			modelSSS.exclude("evaluationSimple", "evaluationComplex", "evaluationExam");
		}
		if (hideClientInfo != null && hideClientInfo) {
			modelSSS.exclude("ipAddress", "clientInfo");
		}
		if (filterTaskInTestInstance != null && filterTaskInTestInstance.getTask().getTaskIsOfTypes() != null
				&& filterTaskInTestInstance.getTask().getTaskIsOfTypes().size() > 1) {
			if (filterTaskInTestInstance.getTask().getTaskIsOfTypes().get(0).getTaskType().getCodetype()
					.equals(ModelConstants.TaskCodeTEXT)) {
				modelSSS.exclude("evaluationSimple", "evaluationComplex", "evaluationExam", "notForEvaluation", "task");
			}
			if (filterTaskInTestInstance.getTask().getTaskIsOfTypes().get(0).getTaskType().getCodetype()
					.equals(ModelConstants.TaskCodeSQL)) {
				modelSSS.exclude("task");
			}
			if (filterTaskInTestInstance.getTask().getTaskIsOfTypes().get(0).getTaskType().getCodetype()
					.equals(ModelConstants.TaskCodeUPLOAD)) {
				modelSSS.exclude("evaluationSimple", "evaluationComplex", "evaluationExam", "notForEvaluation", "task");
			}
			if(filterTaskInTestInstance.getTask().getTaskIsOfTypes().get(0).getTaskType().getCodetype()
				.equals(ModelConstants.TaskCodeTRIGGER)) {
				modelSSS.exclude("task");
			}
		}
		modelSSS.exclude("studentSubmitSolutionId");
		return modelSSS;
	}

	public List<Student> getAllStudents() {
		if (filterTestInstance != null) {
			return testManager.getStudentsWhoStartedTestInstance(filterTestInstance);
		} else {
			return UsefulMethods.castList(Student.class, genericService.getAll(Student.class));
		}
	}

	public SelectModel getSelectTestInstanceModel() {
		List<TestInstance> list = testManager.getAllTestInstances();
		Comparator<TestInstance> comparator = (ti1,
				ti2) -> (ti1 != null && ti2 != null && ti1.getScheduledFor() != null && ti2.getScheduledFor() != null
						? ti1.getScheduledFor().toInstant().truncatedTo(ChronoUnit.DAYS)
								.compareTo(ti2.getScheduledFor().toInstant().truncatedTo(ChronoUnit.DAYS))
						: 0);
		Comparator<TestInstance> reverser = comparator.reversed();
		list.sort(reverser);
		return selectModelFactory.create(list, "title");
	}

	public SelectModel getSelectStudentsModel() {
		return new StudentSelectModel(getAllStudents(), hideNames);
	}

	public SelectModel getSelectTaskInTestInstanceModel() {
		List<TaskInTestInstance> list = new ArrayList<TaskInTestInstance>();
		if (filterTestInstance != null) {
			list = filterTestInstance.getTaskInTestInstances();
		}
		return new TaskInTestInstanceSelectModel(list);
	}

	public void onValueChangedFromSelectStudent(Student newStudent) {
		filterStudent = newStudent;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	public void onValueChangedFromSelectTestInstance(TestInstance ti) {
		filterTestInstance = ti;
		filterTaskInTestInstance = null;
		filterStudent = null;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zTask).addRender(zStudent).addRender(zSubmissions);
		}
	}

	public void onValueChangedFromSelectTaskInTestInstance(TaskInTestInstance tti) {
		filterTaskInTestInstance = tti;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	public void onActionFromShowUserActivities(StudentSubmitSolution selectedSubmission) {
		filterStudent = selectedSubmission.getStudentStartedTest().getStudent();
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	public String getSubmittedByNameWithId() {
		if (hideNames != null && hideNames) {
			return "[" + submission.getStudentStartedTest().getStudent().getPerson().getUserName() + "]";
		} else {
			return personManager.getPersonFullNameWithId(submission.getStudentStartedTest().getStudent().getPerson());
		}
	}

	private void clearResultsAndErrors() {
		resultsEvaluation1 = null;
		resultsEvaluation2 = null;
		resultsErrors1 = null;
		resultsErrors2 = null;
		triggerResultsSimple = null;
		triggerResultsComplex = null;
	}

	@CommitAfter
	public void onReevaluateSubmission(StudentSubmitSolution s) {
		evaluationService.reEvalSolution(userInfo.getUserName(), s);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	@CommitAfter void onReevaluateTriggerSubmission(StudentSubmitSolution s) {
		triggerEvaluationService.reEvalSolution(userInfo.getUserName(), s);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	@CommitAfter
	public void onReevaluateEditedSubmission(StudentSubmitSolution s) {
		clearResultsAndErrors();
		evaluationService.reEvalSolution(userInfo.getUserName(), s);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSQLEval);
		}
	}

	public boolean isEditedAssessmentTaskSQL() {
		if (editedAssessment != null) {
			StudentSubmitSolution sss = genericService.getByPK(StudentSubmitSolution.class,
					editedAssessment.getStudentSubmitSolution().getStudentSubmitSolutionId());
			return TaskTypeChecker.isSQL(testManager.getCodeType(sss));
		} else {
			return false;
		}
	}

	public boolean isEditedAssessmentTaskTrigger() {
		if (editedAssessment != null) {
			StudentSubmitSolution sss = genericService.getByPK(StudentSubmitSolution.class,
				editedAssessment.getStudentSubmitSolution().getStudentSubmitSolutionId());
			return TaskTypeChecker.isTRIGGER(testManager.getCodeType(sss));
		} else {
			return false;
		}
	}

	public boolean isEditedAssessmentTaskSQLorTrigger() {
		return isEditedAssessmentTaskSQL() || isEditedAssessmentTaskTrigger();
	}

	public boolean isSQL() {
		return TaskTypeChecker.isSQL(testManager.getCodeType(submission));
	}

	public boolean isTEXT() {
		return TaskTypeChecker.isTEXT(testManager.getCodeType(submission));
	}

	public boolean isDDL() {
		return TaskTypeChecker.isDDL(testManager.getCodeType(submission));
	}

	public boolean isTrigger() {
		return TaskTypeChecker.isTRIGGER(testManager.getCodeType(submission));
	}

	public boolean isSQLorTrigger() {
		return isSQL() || isTrigger();
	}

	public boolean isFilterTaskDDL() {
		if (filterTaskInTestInstance != null) {
			return filterTaskInTestInstance.getTask().getTaskIsOfTypes().stream()
					.anyMatch(tti -> tti.getTaskType().getCodetype().equals(ModelConstants.TaskCodeDDL));
		} else {
			return false;
		}
	}

	public boolean isUPLOAD() {
		return TaskTypeChecker.isUPLOAD(testManager.getCodeType(submission));
	}

	public SolutionAssessment getSubmissionsFirstEvaluation() {
		return submission.getEvaluations().get(0);
	}

	void onActionFromAddAssessment(StudentSubmitSolution s) {
		clearResultsAndErrors();
		editedAssessment = new SolutionAssessment();
		editedAssessment.setStudentSubmitSolution(s);
		prettyFormat = false;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal);
		}
	}

	void onActionFromEditAssessment(SolutionAssessment sa) {
		editedAssessment = sa;
		prettyFormat = false;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal);
		}
	}

	@CommitAfter
	public void onSuccessFromFormEditor() {
		if (editedAssessment != null) {
			editedAssessment.setEvaluatedOn(Calendar.getInstance().getTime());
			genericService.saveOrUpdate(editedAssessment);
		}
		editedAssessment = null;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal).addRender(zSubmissions);
		}
	}

	public void onActionFromCancelFormEditor() {
		clearResultsAndErrors();
		editedAssessment = null;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal).addRender(zSubmissions);
		}
	}

	public String getEvaluationClass() {
		String output = "";
		try {
			if (submission.getEvaluations() != null && submission.getEvaluations().size() > 0) {
				if (submission.getEvaluations().get(0).getPassed()) {
					output = "correct";
				} else {
					output = "incorrect";
				}
			} else {
				output = "noeval";
			}
		} catch (Exception e) {
			output = "exception";
		}
		return output;
	}

	public String getTranslateTaskDescription() {
		String trans = translationService.getTranslation("Task", "description",
				editedAssessment.getStudentSubmitSolution().getTaskInTestInstance().getTask().getTaskId(),
				persistentLocale.get().getLanguage().toLowerCase());
		if (trans == null) {
			return editedAssessment.getStudentSubmitSolution().getTaskInTestInstance().getTask().getDescription();
		} else {
			return trans;
		}
	}

	void onViewEvaluationResults(StudentSubmitSolution s) {
		if (resultsEvaluation1 != null || resultsEvaluation2 != null) {
			clearResultsAndErrors();
		} else {
			TaskInTestInstance tti = s.getTaskInTestInstance();
			/*
			 * TODO: Only supports TestInstances with a single TestInstanceParameter line
			 */
			TestInstanceParameters tip = tti.getTestInstance().getTestInstanceParameters().get(0);

			Triplet<List<Object[]>, List<String>, List<String>> rsltsSimple = evaluationService
					.getEvalResultsForViewing(userInfo.getUserName(), s.getSubmission(), tti, tip,
							tip.getSchemaSimple());
			Triplet<List<Object[]>, List<String>, List<String>> rsltsComplex = evaluationService
					.getEvalResultsForViewing(userInfo.getUserName(), s.getSubmission(), tti, tip,
							tip.getSchemaComplex());
			resultsEvaluation1 = rsltsSimple.getFirstItem();
			resultsEvaluation2 = rsltsComplex.getFirstItem();
			resultsHeaders1 = rsltsSimple.getSecondItem();
			resultsHeaders2 = rsltsComplex.getSecondItem();
			resultsErrors1 = rsltsSimple.getThirdItem();
			resultsErrors2 = rsltsComplex.getThirdItem();
		}
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSQLEval);
		}
	}

	public void onViewTriggerEvaluationResults(StudentSubmitSolution s) {
		if (triggerResultsSimple != null || triggerResultsComplex != null) {
			clearResultsAndErrors();
		} else {
			TaskInTestInstance tti = s.getTaskInTestInstance();
			TestInstanceParameters tip = tti.getTestInstance().getTestInstanceParameters().get(0);

			long studentId = s.getStudentStartedTest().getStudent().getStudentId();
			String trigger = s.getSubmission();
			String description = tti.getTask().getDescription();

			TriggerResultForPrinting resultSimple = triggerEvaluationService.evalTriggerForPrinting(
				tti,
				tip,
				trigger,
				studentId,
				tip.getSchemaSimple(),
				description
			);

			TriggerResultForPrinting resultComplex = triggerEvaluationService.evalTriggerForPrinting(
				tti,
				tip,
				trigger,
				studentId,
				tip.getSchemaComplex(),
				description
			);

			triggerResultsSimple = resultSimple.getTestCaseResults();
			triggerResultsComplex = resultComplex.getTestCaseResults();
			resultsErrors1 = resultSimple.getExecutionErrors();
			resultsErrors2 = resultComplex.getExecutionErrors();
		}
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSQLEval);
		}
	}

	public String getClassSQLsolution() {
		if (isEditedAssessmentTaskSQL()) {
			return "sqlSolution";
		} else {
			return "nonSqlSolution";
		}
	}

	public void onPrettyFormatToggle() {
		prettyFormat = !prettyFormat;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSolution);
		}
	}

	public String getClassPrettyFormat() {
		if (prettyFormat) {
			return "btn-info";
		} else {
			return " ";
		}
	}

	public String getFormattedSolution() {
		if (isEditedAssessmentTaskSQL() && prettyFormat) {
			return SqlFormatter.of(Dialect.PostgreSql)
					.format(editedAssessment.getStudentSubmitSolution().getSubmission(), FormatConfig.builder()
							.indent("   ").uppercase(true).linesBetweenQueries(2).maxColumnLength(100).build());
		} else {
			return editedAssessment.getStudentSubmitSolution().getSubmission();
		}
	}

	@CommitAfter
	void onAssessAsCorrectSubmission(StudentSubmitSolution submission) {
		SolutionAssessment sa = new SolutionAssessment();
		sa.setStudentSubmitSolution(submission);
		sa.setFeedback(messages.get("assessmentComment-correct"));
		sa.setEvaluatedOn(new Date());
		sa.setGrade(submission.getTaskInTestInstance().getPoints());
		sa.setPassed(true);
		genericService.save(sa);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	@CommitAfter
	void onAssessAsIncorrectSubmission(StudentSubmitSolution submission) {
		SolutionAssessment sa = new SolutionAssessment();
		sa.setStudentSubmitSolution(submission);
		sa.setFeedback(messages.get("assessmentComment-incorrect"));
		sa.setEvaluatedOn(new Date());
		sa.setGrade(0.0F);
		sa.setPassed(false);
		genericService.save(sa);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	@CommitAfter
	void onAssessEditedAsCorrectSubmission() {
		editedAssessment.setFeedback(messages.get("assessmentComment-correct"));
		editedAssessment.setEvaluatedOn(new Date());
		editedAssessment.setGrade(editedAssessment.getStudentSubmitSolution().getTaskInTestInstance().getPoints());
		editedAssessment.setPassed(true);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal);
		}
	}

	@CommitAfter
	void onAssessEditedAsIncorrectSubmission() {
		editedAssessment.setFeedback(messages.get("assessmentComment-incorrect"));
		editedAssessment.setEvaluatedOn(new Date());
		editedAssessment.setGrade(0.0F);
		editedAssessment.setPassed(false);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal);
		}
	}

	public boolean isAnyErrors() {
		return (resultsErrors1 != null && resultsErrors1.size() > 0)
				|| (resultsErrors2 != null && resultsErrors2.size() > 0);
	}

	public boolean isAnyResults() {
		return (resultsEvaluation1 != null) || (resultsEvaluation2 != null);
	}

	public boolean isAnyTriggerResults() {
		return (triggerResultsSimple != null && triggerResultsSimple.size() > 0)
				|| (triggerResultsComplex != null && triggerResultsComplex.size() > 0);
	}

	public boolean isAnyStatus() {
		return isAnyErrors() || isAnyResults() || isAnyTriggerResults();
	}

	void onCopySolutionToFeedback() {
		editedAssessment.setFeedback(getFormattedSolution());
	}

	public String getEditedAssessmentSubmitterName() {
		Person p = editedAssessment.getStudentSubmitSolution().getStudentStartedTest().getStudent().getPerson();
		String name = " [" + p.getUserName() + "]";
		if (hideNames != null && hideNames) {
		} else {
			name = p.getLastName() + " " + p.getFirstName() + name;
		}
		return name;
	}
}
