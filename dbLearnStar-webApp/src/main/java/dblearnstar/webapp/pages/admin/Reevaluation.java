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

import java.util.List;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import dblearnstar.model.entities.StudentSubmitSolution;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TestInstance;
import dblearnstar.model.model.TaskTypeChecker;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.services.EvaluationService;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.TestManager;
import dblearnstar.webapp.services.TriggerEvaluationService;

@AdministratorPage
public class Reevaluation {

	@SessionState
	private UserInfo userInfo;

	@Inject
	private EvaluationService evaluationService;

	@Inject
	private TriggerEvaluationService triggerEvaluationService;

	@Inject
	private GenericService genericService;

	@Inject
	private TestManager testManager;

	@Inject
	private Logger logger;

	public List<StudentSubmitSolution> getSolutionsAll() {
		return evaluationService.getAllSolutionsForEvaluation();
	}

	public List<TestInstance> getAllTestInstances() {
		return testManager.getAllTestInstances();
	}

	@Property
	private TestInstance testInstance;

	@Property
	private TaskInTestInstance taskInTestInstance;

	@Inject
	Request request;

	public void onActivate() {
		logger.info("activated from {} by {} {}", request.getRemoteHost(), userInfo.getUserName(),
				request.getHeader("User-Agent"));
	}

	@CommitAfter
	public void onActionFromReEvalTest(TestInstance selectedTestInstance) {
		List<StudentSubmitSolution> list = evaluationService
				.getAllSolutionsForEvalutionFromTestInstance(selectedTestInstance);

		List<StudentSubmitSolution> solutions = list.stream()
				.filter(solution -> !TaskTypeChecker.isTRIGGER(testManager.getCodeType(solution)))
				.toList();
		List<StudentSubmitSolution> triggerSolutions = list.stream()
				.filter(solution -> TaskTypeChecker.isTRIGGER(testManager.getCodeType(solution)))
				.toList();

		evaluationService.reEvalListOfSubmissions(userInfo.getUserName(), solutions);
		triggerEvaluationService.reEvalListOfSubmissions(userInfo.getUserName(), triggerSolutions);

		logger.info("Finished reEvalTest and processed {} entries", list.size());
	}

	@CommitAfter
	public void onActionFromReEvalTaskInTestInstance(TaskInTestInstance taskInTestInstance) {
		List<StudentSubmitSolution> list = evaluationService
				.getAllSolutionsForEvalutionFromTaskInTestInstance(taskInTestInstance);

		List<StudentSubmitSolution> solutions = list.stream()
				.filter(solution -> !TaskTypeChecker.isTRIGGER(testManager.getCodeType(solution)))
				.toList();
		List<StudentSubmitSolution> triggerSolutions = list.stream()
				.filter(solution -> TaskTypeChecker.isTRIGGER(testManager.getCodeType(solution)))
				.toList();

		evaluationService.reEvalListOfSubmissions(userInfo.getUserName(), solutions);
		triggerEvaluationService.reEvalListOfSubmissions(userInfo.getUserName(), triggerSolutions);

		logger.info("Finished reEvalTaskInTestInstance and processed {} entries", list.size());
	}

	@CommitAfter
	public void onActionFromReEvalAll() {
		List<StudentSubmitSolution> list = evaluationService.getAllSolutionsForEvaluation();

		List<StudentSubmitSolution> solutions = list.stream()
				.filter(solution -> !TaskTypeChecker.isTRIGGER(testManager.getCodeType(solution)))
				.toList();
		List<StudentSubmitSolution> triggerSolutions = list.stream()
				.filter(solution -> TaskTypeChecker.isTRIGGER(testManager.getCodeType(solution)))
				.toList();

		evaluationService.reEvalListOfSubmissions(userInfo.getUserName(), solutions);
		triggerEvaluationService.reEvalListOfSubmissions(userInfo.getUserName(), triggerSolutions);

		logger.info("Finished reEvalAll and processed {} entries", list.size());
	}

}
