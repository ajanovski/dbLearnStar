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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tapestry5.OptionGroupModel;
import org.apache.tapestry5.OptionModel;
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
import org.apache.tapestry5.internal.OptionModelImpl;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PersistentLocale;
import org.apache.tapestry5.services.SelectModelFactory;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.util.AbstractSelectModel;
import org.slf4j.Logger;

import dblearnstar.model.entities.SolutionAssessment;
import dblearnstar.model.entities.Student;
import dblearnstar.model.entities.StudentSubmitSolution;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TaskIsOfType;
import dblearnstar.model.entities.TestInstance;
import dblearnstar.model.model.ModelConstants;
import dblearnstar.model.model.TaskTypeChecker;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.model.StudentSelectModel;
import dblearnstar.webapp.services.EvaluationService;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.PersonManager;
import dblearnstar.webapp.services.TestManager;
import dblearnstar.webapp.services.TranslationService;
import dblearnstar.webapp.services.UsefulMethods;

@AdministratorPage
@Import(stylesheet = { "SubmissionLogViewer.css" }, module = { "zoneUpdateEffect", "bootstrap/modal",
		"bootstrap/collapse" })
public class SubmissionLogViewer {

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

	@Property
	private StudentSubmitSolution submission;

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
	private Boolean hideClientInfo;
	@Persist
	@Property
	SolutionAssessment editedAssessment;

	private Boolean toCancel;

	public void onActivate() {
		logger.info("Activated from {} by {} {}", request.getRemoteHost(), userInfo.getUserName(),
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
						? ti1.getScheduledFor().compareTo(ti2.getScheduledFor())
						: 0);
		Comparator<TestInstance> reverser = comparator.reversed();
		list.sort(reverser);
		return selectModelFactory.create(list, "title");
	}

	public SelectModel getSelectStudentsModel() {
		return new StudentSelectModel(getAllStudents());
	}

	public SelectModel getSelectTaskInTestInstanceModel() {

		class TaskInTestInstanceSelectModel extends AbstractSelectModel {
			private List<TaskInTestInstance> taskInTestInstances;

			public TaskInTestInstanceSelectModel(List<TaskInTestInstance> taskInTestInstances) {
				if (taskInTestInstances == null) {
					this.taskInTestInstances = new ArrayList<TaskInTestInstance>();
				} else {
					this.taskInTestInstances = taskInTestInstances;
				}
			}

			@Override
			public List<OptionGroupModel> getOptionGroups() {
				return null;
			}

			@Override
			public List<OptionModel> getOptions() {
				List<OptionModel> options = new ArrayList<OptionModel>();
				for (TaskInTestInstance taskInTestInstance : taskInTestInstances) {
					options.add(new OptionModelImpl(taskInTestInstance.getTask().getTitle(), taskInTestInstance));
				}
				return options;
			}
		}

		if (filterTestInstance == null) {
			return new TaskInTestInstanceSelectModel(new ArrayList<TaskInTestInstance>());
		} else {
			return new TaskInTestInstanceSelectModel(filterTestInstance.getTaskInTestInstances());
		}
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
		return personManager.getPersonFullNameWithId(submission.getStudentStartedTest().getStudent().getPerson());
	}

	@CommitAfter
	public void onActionFromReevaluateSubmission(StudentSubmitSolution s) {
		evaluationService.processSolution(userInfo.getUserName(), s);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zSubmissions);
		}
	}

	public String getCodeType() {
		List<TaskIsOfType> listTypes = submission.getTaskInTestInstance().getTask().getTaskIsOfTypes();
		if (listTypes != null && listTypes.size() > 0) {
			return listTypes.get(0).getTaskType().getCodetype();
		} else {
			return "/";
		}
	}

	public boolean isSQL() {
		return TaskTypeChecker.isSQL(getCodeType());
	}

	public boolean isTEXT() {
		return TaskTypeChecker.isTEXT(getCodeType());
	}

	public boolean isDDL() {
		return TaskTypeChecker.isDDL(getCodeType());
	}

	public boolean isUPLOAD() {
		return TaskTypeChecker.isUPLOAD(getCodeType());
	}

	public SolutionAssessment getSubmissionsFirstEvaluation() {
		return submission.getEvaluations().get(0);
	}

	void onActionFromAddAssessment(StudentSubmitSolution s) {
		editedAssessment = new SolutionAssessment();
		editedAssessment.setStudentSubmitSolution(s);
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal);
		}
	}

	void onActionFromEditAssessment(SolutionAssessment sa) {
		editedAssessment = sa;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(zModal);
		}
	}

	public void onCanceledFromFormEditor() {
		toCancel = true;
	}

	@CommitAfter
	public void onSubmitFromFormEditor() {
		if (toCancel != null && toCancel) {
			toCancel = null;
		} else {
			if (editedAssessment != null) {
				editedAssessment.setEvaluatedOn(Calendar.getInstance().getTime());
				genericService.saveOrUpdate(editedAssessment);
			}
		}
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

}
