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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PersistentLocale;
import org.apache.tapestry5.services.SelectModelFactory;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import dblearnstar.model.entities.Model;
import dblearnstar.model.entities.Student;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TaskType;
import dblearnstar.model.model.TaskTypeChecker;
import dblearnstar.model.entities.TestInstance;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.annotations.StudentPage;
import dblearnstar.webapp.pages.admin.SubmissionEvaluations;
import dblearnstar.webapp.services.DigestService;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.PersonManager;
import dblearnstar.webapp.services.TestManager;
import dblearnstar.webapp.services.TranslationService;

@AdministratorPage
@StudentPage
@Import(stylesheet = "ExamsAndTasksOverviewPage.css", module = "bootstrap/collapse")

public class TasksOverviewPage {
	@Inject
	private Logger logger;
	@Inject
	private JavaScriptSupport javaScriptSupport;
	@Inject
	private PersistentLocale persistentLocale;
	@Inject
	private SelectModelFactory selectModelFactory;
	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;
	@Inject
	private DigestService digestService;

	@SessionState
	@Property
	private UserInfo userInfo;

	@Persist
	@Property
	private Model chosenModel;

	@Property
	private TaskInTestInstance listedTaskInTestInstance;

	@Persist
	@Property
	private TaskType chosenTaskType;

	@Inject
	private PersonManager pm;
	@Inject
	private GenericService genericService;
	@Inject
	private TestManager testManager;
	@Inject
	private TranslationService translationService;

	@InjectPage
	private SubmissionEvaluations submissionEvaluations;

	@Property
	private Model listedModel;
	@Property
	private TaskType listedTaskType;

	private Student student;

	public void onActivate() {
		// @TODO better implementation is needed for many students per user
		student = pm.getStudentsByPersonId(userInfo.getPersonId()).get(0);
		prevTestInstance = null;
	}

	public List<Model> getAllModels() {
		List<Model> list = testManager.getAllModels();
		return list.stream().sorted((o1, o2) -> {
			return o1.getTitle().compareTo(o2.getTitle());
		}).toList();
	}

	public List<TaskType> getAllTaskTypes() {
		List<TaskType> list = new ArrayList<TaskType>();
		if (chosenModel != null) {
			list = testManager.getAllTaskTypesDefinedOverModel(chosenModel);
		} else {
			list = testManager.getAllTaskTypes();
		}
		return list.stream().sorted((o1, o2) -> {
			return o1.getTitle().compareTo(o2.getTitle());
		}).toList();
	}

	public String getClassSelectedModel() {
		if (chosenModel != null) {
			if (listedModel.getModelId() == chosenModel.getModelId()) {
				return " btn-primary text-light ";
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	public String getClassSelectedTaskType() {
		if (chosenTaskType != null) {
			if (listedTaskType.getTaskTypeId() == chosenTaskType.getTaskTypeId()) {
				return " btn-primary text-light ";
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	public void onChooseModel(Model m) {
		chosenModel = m;
	}

	public void onUnchooseModel() {
		chosenModel = null;
	}

	public void onChooseTaskType(TaskType tt) {
		chosenTaskType = tt;
	}

	public void onUnchooseTaskType() {
		chosenTaskType = null;
	}

	private TestInstance prevTestInstance;

	public Boolean getPrevTestInstance() {
		if (prevTestInstance == null) {
			prevTestInstance = listedTaskInTestInstance.getTestInstance();
			return true;
		} else {
			if (prevTestInstance.getTestInstanceId() == listedTaskInTestInstance.getTestInstance()
					.getTestInstanceId()) {
				return false;
			} else {
				prevTestInstance = listedTaskInTestInstance.getTestInstance();
				return true;
			}
		}
	}

	public List<TaskInTestInstance> getAllTasks() {
		if (chosenModel != null) {
			List<TaskInTestInstance> list = testManager.getTaskInTestInstancesByModel(chosenModel.getModelId());
			if (chosenTaskType != null) {
				list = list.stream()
						.filter(p -> p.getTask().getTaskIsOfTypes().stream()
								.anyMatch(q -> q.getTaskType().getTaskTypeId() == chosenTaskType.getTaskTypeId()))
						.collect(Collectors.toList());
				if (student != null) {
					list = list.stream().filter(p -> testManager.accessToTaskInTestInstanceAllowed(student, p))
							.toList();
				}
			}
			return list;
		} else {
			if (chosenTaskType != null) {
				List<TaskInTestInstance> list = new ArrayList<TaskInTestInstance>();
				list = testManager.getTaskInTestInstancesByTaskType(chosenTaskType);
				if (student != null) {
					list = list.stream().filter(p -> testManager.accessToTaskInTestInstanceAllowed(student, p))
							.toList();
				}
				return list;
			} else {
				return null;
			}
		}
	}

	public Long getNumPersonsSuccessful() {
		return testManager
				.getNumPersonsSuccessfulForTaskInTestInstance(listedTaskInTestInstance.getTaskInTestInstanceId());
	}

	public long getNumPersonsTriedToSolve() {
		return testManager
				.getNumPersonsTriedToSolveTaskInTestInstance(listedTaskInTestInstance.getTaskInTestInstanceId());
	}

	public String getClassForSolved() {
		Boolean solved = isTaskInTestInstanceSolved();
		if (solved != null && solved) {
			return "text-white bg-success";
		} else {
			return "";
		}
	}

	public Boolean isTaskInTestInstanceSolved() {
		if (student != null) {
			return testManager.isTaskInTestInstanceSolvedByStudent(listedTaskInTestInstance.getTaskInTestInstanceId(),
					student.getStudentId());
		} else {
			return false;
		}
	}

	public boolean isTriggerTask() {
		return listedTaskInTestInstance
			.getTask()
			.getTaskIsOfTypes()
			.stream()
			.anyMatch(t -> TaskTypeChecker.isTRIGGER(t.getTaskType().getCodetype()));
	}

	public String parsedListedTaskDescription() {
		if(!isTriggerTask()) {
			return listedTaskInTestInstance.getTask().getDescription();
		} else {
			String description = listedTaskInTestInstance.getTask().getDescription();
			return description.split("###")[0].trim();
		}
	}
}
